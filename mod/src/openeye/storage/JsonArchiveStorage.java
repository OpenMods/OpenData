package openeye.storage;

import java.io.File;

import argo.jdom.JsonRootNode;

import com.google.common.base.Preconditions;

public class JsonArchiveStorage extends JsonStorageBase implements IAppendableStorage<JsonRootNode> {

	private final File dir;
	private final String prefix;

	public JsonArchiveStorage(File dir, String prefix) {
		Preconditions.checkArgument(dir.isDirectory());
		this.dir = dir;
		this.prefix = prefix;
	}

	@Override
	public IDataSource<JsonRootNode> createNew() {
		return createNew(generateId());
	}

	@Override
	public IDataSource<JsonRootNode> createNew(String id) {
		String filename = generateFilename(prefix, id);
		File file = new File(dir, filename);

		IDataSource<JsonRootNode> newSource = createFromFile(id, file);
		return newSource;
	}

	@Override
	protected void removeEntry(String id) {
		// NO-OP
	}

}
