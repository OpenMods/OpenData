package openeye.storage;

import java.io.File;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;

public abstract class GsonDirStorage<T> extends GsonStorageBase<T> implements IAppendableStorage<T> {

	protected final File dir;

	protected final String prefix;

	protected GsonDirStorage(File dir, String prefix, Class<? extends T> cls, Gson gson, String extension) {
		super(cls, gson, extension);
		Preconditions.checkArgument(dir.isDirectory());
		this.dir = dir;
		this.prefix = prefix;
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
}
