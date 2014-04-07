package openeye.storage;

import java.io.File;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;

public class GsonArchiveStorage<T> extends GsonStorageBase<T> implements IAppendableStorage<T> {

	private final File dir;
	private final String prefix;

	public GsonArchiveStorage(File dir, String prefix, Class<? extends T> cls, Gson gson) {
		super(cls, gson);
		Preconditions.checkArgument(dir.isDirectory());
		this.dir = dir;
		this.prefix = prefix;
	}

	@Override
	public IDataSource<T> createNew() {
		return createNew(generateId());
	}

	@Override
	public IDataSource<T> createNew(String id) {
		String filename = generateFilename(prefix, id);
		File file = new File(dir, filename);

		IDataSource<T> newSource = createFromFile(id, file);
		return newSource;
	}

	@Override
	protected void removeEntry(String id) {
		// NO-OP
	}

}
