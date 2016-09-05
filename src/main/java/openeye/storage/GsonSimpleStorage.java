package openeye.storage;

import java.io.File;
import java.io.InputStream;

import com.google.gson.Gson;

public class GsonSimpleStorage<T> extends GsonDirStorage<T> {

	public GsonSimpleStorage(File dir, String prefix, String extension, Class<? extends T> cls, Gson gson) {
		super(dir, prefix, cls, gson, extension);
	}

	@Override
	protected void removeEntry(String id) {
		// NO-OP
	}

	@Override
	protected InputStream createInputStream(File file) {
		throw new UnsupportedOperationException();
	}
}
