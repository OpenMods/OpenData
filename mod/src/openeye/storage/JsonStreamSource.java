package openeye.storage;

import java.io.*;
import java.nio.charset.StandardCharsets;

import openeye.Log;
import argo.format.JsonFormatter;
import argo.format.PrettyJsonFormatter;
import argo.jdom.JdomParser;
import argo.jdom.JsonRootNode;

import com.google.common.io.Closer;

public abstract class JsonStreamSource implements IDataSource<JsonRootNode> {

	private static final JsonFormatter JSON_SERIALIZER = new PrettyJsonFormatter();
	private static final JdomParser JDOM_PARSER = new JdomParser();

	protected final String id;

	protected abstract InputStream createInputStream();

	protected abstract OutputStream createOutputStream();

	protected abstract String description();

	protected abstract boolean sourceExists();

	public JsonStreamSource(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public JsonRootNode retrieve() {
		if (!sourceExists()) return null;

		try {
			final Closer closer = Closer.create();
			try {
				InputStream input = closer.register(createInputStream());
				Reader reader = closer.register(new InputStreamReader(input, StandardCharsets.UTF_8));
				return JDOM_PARSER.parse(reader);
			} finally {
				closer.close();
			}
		} catch (Throwable t) {
			Log.severe(t, "Failed to save JSON data to file %s (id: %s)", description(), id);
			return null;
		}
	}

	@Override
	public void store(JsonRootNode value) {
		try {
			final Closer closer = Closer.create();
			try {
				OutputStream output = closer.register(createOutputStream());
				Writer writer = closer.register(new OutputStreamWriter(output, StandardCharsets.UTF_8));
				JSON_SERIALIZER.format(value, writer);
			} finally {
				closer.close();
			}
		} catch (Throwable t) {
			Log.warn(t, "Failed to save JSON data to file %s (id: %s)", description(), id);
		}
	}
}
