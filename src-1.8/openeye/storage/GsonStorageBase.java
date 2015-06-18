package openeye.storage;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import openeye.Log;

import com.google.common.base.Throwables;
import com.google.gson.Gson;

public abstract class GsonStorageBase<T> {

	private static final DateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

	protected final Class<? extends T> cls;

	protected final Gson gson;

	protected final String extension;

	protected GsonStorageBase(Class<? extends T> cls, Gson gson, String extension) {
		this.cls = cls;
		this.gson = gson;
		this.extension = extension;
	}

	protected static String generateId() {
		return FORMATTER.format(new Date());
	}

	protected String generateFilename(String prefix, String id) {
		return String.format("%s-%s.%s", prefix, id, extension);
	}

	protected IDataSource<T> createFromFile(String id, final File file) {
		return new GsonStreamSource<T>(id, cls, gson) {
			@Override
			public void delete() {
				try {
					file.delete();
					removeEntry(id);
				} catch (Throwable t) {
					Log.warn(t, "Can't delete file %s", file);
				}
			}

			@Override
			protected String description() {
				return file.getAbsolutePath();
			}

			@Override
			protected OutputStream createOutputStream() {
				try {
					return GsonStorageBase.this.createOutputStream(file);
				} catch (Throwable t) {
					throw Throwables.propagate(t);
				}
			}

			@Override
			protected InputStream createInputStream() {
				try {
					return GsonStorageBase.this.createInputStream(file);
				} catch (Throwable t) {
					throw Throwables.propagate(t);
				}
			}

			@Override
			protected boolean sourceExists() {
				return file.exists();
			}
		};
	}

	protected abstract void removeEntry(String id);

	protected OutputStream createOutputStream(final File file) throws IOException {
		return new FileOutputStream(file);
	}

	protected InputStream createInputStream(final File file) throws IOException {
		return new FileInputStream(file);
	}

}
