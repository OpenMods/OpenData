package openeye.storage;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import openeye.Log;
import argo.jdom.JsonRootNode;

import com.google.common.base.Throwables;

public abstract class JsonStorageBase {

	private static final DateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

	protected static String generateId() {
		return FORMATTER.format(new Date());
	}

	protected String generateFilename(String prefix, String id) {
		return String.format("%s-%s.json", prefix, id);
	}

	protected IDataSource<JsonRootNode> createFromFile(String id, final File file) {
		return new JsonStreamSource(id) {
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
					return new FileOutputStream(file);
				} catch (Throwable t) {
					throw Throwables.propagate(t);
				}
			}

			@Override
			protected InputStream createInputStream() {
				try {
					return new FileInputStream(file);
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

}
