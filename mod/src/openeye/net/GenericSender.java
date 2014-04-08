package openeye.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

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

	public final URL url;

	private int retries = 3;

	private int timeout = 1000;

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
				HttpURLConnection connection = (HttpURLConnection)url.openConnection();
				connection.setDoInput(true);
				connection.setDoOutput(true);
				connection.setRequestMethod("POST");
				connection.setConnectTimeout(timeout);
				connection.setReadTimeout(timeout);
				connection.setRequestProperty("Accept", "application/json");
				// connection.setRequestProperty("Content-Encoding", "gzip");
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setRequestProperty("User-Agent", "Die Fledermaus/11");
				connection.setRequestProperty("Host", url.getHost() + ":" + url.getPort());
				connection.setInstanceFollowRedirects(true);

				OutputStream requestStream = connection.getOutputStream();
				// requestStream = new GZIPOutputStream(requestStream);
				encodeRequest(requestStream, request);
				requestStream.flush();
				requestStream.close();

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

				InputStream stream = connection.getInputStream();
				O response = decodeResponse(stream);
				stream.close();

				return response;
			} catch (SocketTimeoutException e) {
				Log.warn("Connection to %s timed out (retry %d)", url, retry);
			} catch (Throwable t) {
				Log.warn(t, "Failed to send report to %s on retry %d", url, retry);
			}
		}
		throw new FailedToSend();
	}

	protected abstract void encodeRequest(OutputStream output, I request) throws IOException;

	protected abstract O decodeResponse(InputStream input) throws IOException;

}
