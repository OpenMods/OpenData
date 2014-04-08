package openeye.storage;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;

public class GsonArchiveStorage<T> extends GsonStorageBase<T> implements IAppendableStorage<T> {

	private final File dir;
	private final String prefix;
	private final String fileName;

	public GsonArchiveStorage(File dir, String prefix, String fileName, Class<? extends T> cls, Gson gson) {
		super(cls, gson, "zip");
		Preconditions.checkArgument(dir.isDirectory());
		this.dir = dir;
		this.prefix = prefix;
		this.fileName = fileName;
	}

	@Override
	public IDataSource<T> createNew() {
		String prefixId = generateId();
		String id;
		File file;
		int count = 0;
		do {
			id = prefixId + "-" + count++;
			String filename = generateFilename(prefix, id);
			file = new File(dir, filename);
		} while (file.exists());

		return createFromFile(id, file);
	}

	@Override
	public IDataSource<T> createNew(String id) {
		String filename = generateFilename(prefix, id);
		File file = new File(dir, filename);
		return createFromFile(id, file);
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
