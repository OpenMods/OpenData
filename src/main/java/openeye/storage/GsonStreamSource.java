package openeye.storage;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

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
