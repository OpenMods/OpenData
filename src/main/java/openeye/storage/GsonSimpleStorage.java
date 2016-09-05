package openeye.storage;

import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class GsonSimpleStorage<T> extends GsonDirStorage<T> {

	public GsonSimpleStorage(File dir, String prefix, String extension, Class<? extends T> cls, Gson gson) {
		super(dir, prefix, cls, gson, extension);
	}

	@Override
	protected void removeEntry(String id) {
		// NO-OP
	}

	@Override
	protected InputStream createInputStream(File file) throws IOException {
		throw new UnsupportedOperationException();
	}
}
