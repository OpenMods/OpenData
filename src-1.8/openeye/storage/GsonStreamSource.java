package openeye.storage;

import java.io.*;

import com.google.common.base.Charsets;
import com.google.gson.Gson;

public abstract class GsonStreamSource<T> implements IDataSource<T> {

	protected final String id;

	protected final Gson gson;

	protected final Class<? extends T> cls;

	protected abstract InputStream createInputStream();

	protected abstract OutputStream createOutputStream();

	protected abstract String description();

	protected abstract boolean sourceExists();

	protected void afterWrite(Writer writer) throws IOException {
		writer.close();
	}

	protected void afterRead(Reader reader) throws IOException {
		reader.close();
	}

	public GsonStreamSource(String id, Class<? extends T> cls, Gson gson) {
		this.id = id;
		this.gson = gson;
		this.cls = cls;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public T retrieve() {
		if (!sourceExists()) return null;

		try {
			InputStream input = createInputStream();
			Reader reader = new InputStreamReader(input, Charsets.UTF_8);
			try {
				return gson.fromJson(reader, cls);
			} finally {
				afterRead(reader);
			}
		} catch (Throwable t) {
			throw new IllegalStateException(String.format("Failed to read JSON data from file %s (id: %s)", description(), id), t);
		}
	}

	@Override
	public void store(T value) {
		try {
			OutputStream output = createOutputStream();
			Writer writer = new OutputStreamWriter(output, Charsets.UTF_8);
			try {
				gson.toJson(value, writer);
			} finally {
				afterWrite(writer);
			}
		} catch (Throwable t) {
			throw new IllegalStateException(String.format("Failed to save JSON data to file %s (id: %s)", description(), id), t);
		}
	}
}
