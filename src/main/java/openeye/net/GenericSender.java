package openeye.net;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import openeye.Log;
import org.apache.commons.lang3.tuple.Pair;

public abstract class GenericSender<I, O> {

	private final List<String> bundledRoots = ImmutableList.of("isrg_root_x1.pem", "identrust_root_x3.pem");

	private SSLSocketFactory createSocketFactoryWithRoots(List<String> roots) throws GeneralSecurityException, IOException {
		final String defaultKsAlgorithm = KeyStore.getDefaultType();
		KeyStore keyStore = KeyStore.getInstance(defaultKsAlgorithm);

		// for 'full' keystore
		// Path ksPath = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
		// keyStore.load(Files.newInputStream(ksPath), "changeit".toCharArray());

		// for single cert keystore
		keyStore.load(null);

		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

		for (String root : roots) {
			InputStream data = getClass().getClassLoader().getResourceAsStream(root);
			Preconditions.checkNotNull(data, "Failed to found resource %s", root);
			Certificate cert = certificateFactory.generateCertificate(data);
			keyStore.setCertificateEntry(root, cert);
		}

		final String defaultTmAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(defaultTmAlgorithm);
		tmf.init(keyStore);

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, tmf.getTrustManagers(), null);

		return sslContext.getSocketFactory();
	}

	@SuppressWarnings("serial")
	public static class HttpTransactionException extends RuntimeException {
		private HttpTransactionException(String format, Object... args) {
			super(String.format(format, args));
		}

		private HttpTransactionException(Throwable cause) {
			super(cause);
		}
	}

	public enum EncryptionState {
		NOT_SUPPORTED,
		NO_ROOT_CERTIFICATE,
		OK,
		UNKNOWN;
	}

	private final String host;

	private final String path;

	private int retries = 2;

	private int timeout = 20000;

	private EncryptionState encryptionState = EncryptionState.UNKNOWN;

	public GenericSender(String host, String path) {
		this.host = host;
		this.path = path;
	}

	public void setRetries(int retries) {
		this.retries = retries;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public EncryptionState getEncryptionState() {
		return encryptionState;
	}

	public O sendAndReceive(I request) {
		for (int retry = 0; retry < retries; retry++) {
			try {
				final HttpURLConnection connection;
				try {
					final Pair<HttpURLConnection, EncryptionState> result = createConnection();
					encryptionState = Ordering.natural().min(result.getRight(), encryptionState);
					connection = result.getLeft();
				} catch (GeneralSecurityException t) {
					// giving up, something broken in encryption
					throw new HttpTransactionException(t);
				}

				trySendRequest(request, connection);
				checkStatusCode(connection);
				return tryReceiveResponse(connection);
			} catch (HttpTransactionException e) {
				throw e;
			} catch (SocketTimeoutException e) {
				Log.warn("Connection timed out (retry %d)", retry);
			} catch (Throwable t) {
				Log.warn(t, "Failed to send/receive report (retry %d)", retry);
			}
		}

		throw new HttpTransactionException("Too much retries");
	}

	private Pair<HttpURLConnection, EncryptionState> createConnection() throws IOException, GeneralSecurityException {
		// non-business versions of Java 6 can't handle our awesome certificates
		if (System.getProperty("java.specification.version").equals("1.6")) {
			final URL url = new URL("http", host, path);
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			configureAndConnect(url, connection);
			return Pair.of(connection, EncryptionState.NOT_SUPPORTED);
		} else {
			final URL url = new URL("https", host, path);
			try {
				HttpURLConnection connection = (HttpURLConnection)url.openConnection();
				configureAndConnect(url, connection);
				return Pair.of(connection, EncryptionState.OK);
			} catch (SSLHandshakeException e) {
				HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
				final SSLSocketFactory sslSocketFactory = createSocketFactoryWithRoots(bundledRoots);
				connection.setSSLSocketFactory(sslSocketFactory);
				configureAndConnect(url, connection);
				return Pair.of((HttpURLConnection)connection, EncryptionState.NO_ROOT_CERTIFICATE);
			}
		}
	}

	private void configureAndConnect(URL url, HttpURLConnection connection) throws ProtocolException, IOException {
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");
		connection.setConnectTimeout(timeout);
		connection.setReadTimeout(timeout);
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestProperty("Content-Encoding", "gzip");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("User-Agent", "Die Fledermaus/11");
		connection.setRequestProperty("Host", url.getAuthority());
		connection.setInstanceFollowRedirects(true);
		connection.connect();
	}

	protected void trySendRequest(I request, URLConnection connection) throws IOException {
		OutputStream requestStream = connection.getOutputStream();
		requestStream = new GZIPOutputStream(requestStream);

		try {
			encodeRequest(requestStream, request);
			requestStream.flush();
		} finally {
			requestStream.close();
		}
	}

	protected void checkStatusCode(HttpURLConnection connection) throws IOException {
		int statusCode = connection.getResponseCode();
		switch (statusCode) {
			case HttpURLConnection.HTTP_OK:
				break;
			case HttpURLConnection.HTTP_NOT_FOUND:
				throw new HttpTransactionException("Endpoint not found");
			case HttpURLConnection.HTTP_INTERNAL_ERROR:
				throw new HttpTransactionException("Internal server error");
			default:
				throw new HttpTransactionException("HttpStatus %d != 200", statusCode);
		}
	}

	protected O tryReceiveResponse(HttpURLConnection connection) throws IOException {
		InputStream stream = connection.getInputStream();

		try {
			return decodeResponse(stream);
		} finally {
			stream.close();
		}
	}

	protected abstract void encodeRequest(OutputStream output, I request) throws IOException;

	protected abstract O decodeResponse(InputStream input) throws IOException;

}
