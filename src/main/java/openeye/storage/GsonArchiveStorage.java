package openeye.storage;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.gson.Gson;

public class GsonArchiveStorage<T> extends GsonDirStorage<T> {

	private final String fileName;

	public GsonArchiveStorage(File dir, String prefix, String fileName, Class<? extends T> cls, Gson gson) {
		super(dir, prefix, cls, gson, "zip");
		this.fileName = fileName;
	}

	@Override
	protected void removeEntry(String id) {
		// NO-OP
	}

	@Override
	protected OutputStream createOutputStream(File file) throws IOException {
		OutputStream originalOutput = super.createOutputStream(file);
		ZipOutputStream zipOutput = new ZipOutputStream(originalOutput);
		zipOutput.setLevel(9);
		zipOutput.putNextEntry(new ZipEntry(fileName));
		return zipOutput;
	}

	@Override
	protected InputStream createInputStream(File file) {
		throw new UnsupportedOperationException();
	}

}
