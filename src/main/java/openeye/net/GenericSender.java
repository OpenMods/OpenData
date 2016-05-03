package openeye.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.zip.GZIPOutputStream;

import openeye.Log;

import com.google.common.base.Throwables;

public abstract class GenericSender<I, O> {

	public static class FailedToSend extends RuntimeException {
		private static final long serialVersionUID = -5969369994481708159L;

		private FailedToSend() {}

		private FailedToSend(String format, Object... args) {
			super(String.format(format, args));
		}
	}

	public static class FailedToReceive extends RuntimeException {
		private static final long serialVersionUID = -4347813571109953877L;

		private FailedToReceive(Throwable cause) {
			super(cause);
		}

		private FailedToReceive(String format, Object... args) {
			super(String.format(format, args));
		}
	}

	public static class Retry extends RuntimeException {
		private static final long serialVersionUID = -4620199692564394387L;
	}

	public final URL url;

	private int retries = 2;

	private int timeout = 20000;

	protected GenericSender(URL url) {
		this.url = url;
	}

	protected GenericSender(String url) {
		try {
			this.url = new URL(url);
		} catch (MalformedURLException e) {
			throw Throwables.propagate(e);
		}
	}

	public void setRetries(int retries) {
		this.retries = retries;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public O sendAndReceive(I request) {
		for (int retry = 0; retry < retries; retry++) {
			try {
				HttpURLConnection connection = createConnection(url, timeout);
				trySendRequest(request, connection);
				checkStatusCode(connection);
				return tryReceiveResponse(connection);
			} catch (FailedToSend e) {
				throw e;
			} catch (FailedToReceive e) {
				throw e;
			} catch (Retry e) {
				Log.warn("Retrying sending request");
			} catch (SocketTimeoutException e) {
				Log.warn("Connection to %s timed out (retry %d)", url, retry);
			} catch (Throwable t) {
				Log.warn(t, "Failed to send report to %s on retry %d", url, retry);
			}
		}
		throw new FailedToSend("Too much retries");
	}

	private static HttpURLConnection createConnection(URL url, int timeout) throws IOException, ProtocolException {
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");
		connection.setConnectTimeout(timeout);
		connection.setReadTimeout(timeout);
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestProperty("Content-Encoding", "gzip");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("User-Agent", "Die Fledermaus/11");
		connection.setRequestProperty("Host", url.getHost() + ":" + url.getPort());
		connection.setInstanceFollowRedirects(true);
		return connection;
	}

	protected void trySendRequest(I request, HttpURLConnection connection) throws IOException {
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
				throw new FailedToSend("Endpoint %s not found", url);
			case HttpURLConnection.HTTP_INTERNAL_ERROR:
				throw new FailedToSend("Internal server error for url %s", url);
			default:
				throw new FailedToSend("HttpStatus %d != 200", statusCode);
		}
	}

	protected O tryReceiveResponse(HttpURLConnection connection) throws IOException {
		InputStream stream = connection.getInputStream();

		try {
			return decodeResponse(stream);
		} catch (Throwable t) {
			throw new FailedToReceive(t);
		} finally {
			stream.close();
		}
	}

	protected abstract void encodeRequest(OutputStream output, I request) throws IOException;

	protected abstract O decodeResponse(InputStream input) throws IOException;

}
