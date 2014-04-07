package openeye.storage;

import java.io.*;
import java.nio.charset.StandardCharsets;

import openeye.Log;

import com.google.common.io.Closer;
import com.google.gson.Gson;

public abstract class GsonStreamSource<T> implements IDataSource<T> {

	protected final String id;

	protected final Gson gson;

	protected final Class<? extends T> cls;

	protected abstract InputStream createInputStream();

	protected abstract OutputStream createOutputStream();

	protected abstract String description();

	protected abstract boolean sourceExists();

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
			final Closer closer = Closer.create();
			try {
				InputStream input = closer.register(createInputStream());
				Reader reader = closer.register(new InputStreamReader(input, StandardCharsets.UTF_8));
				return gson.fromJson(reader, cls);
			} finally {
				closer.close();
			}
		} catch (Throwable t) {
			Log.severe(t, "Failed to save JSON data to file %s (id: %s)", description(), id);
			return null;
		}
	}

	@Override
	public void store(T value) {
		try {
			final Closer closer = Closer.create();
			try {
				OutputStream output = closer.register(createOutputStream());
				Writer writer = closer.register(new OutputStreamWriter(output, StandardCharsets.UTF_8));
				gson.toJson(value, writer);
			} finally {
				closer.close();
			}
		} catch (Throwable t) {
			Log.warn(t, "Failed to save JSON data to file %s (id: %s)", description(), id);
		}
	}
}
