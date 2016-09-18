package openeye.storage;

import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
	protected InputStream createInputStream(File file) throws IOException {
		throw new UnsupportedOperationException();
	}

}
