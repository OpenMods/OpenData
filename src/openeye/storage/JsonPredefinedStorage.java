package openeye.storage;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import argo.jdom.JsonRootNode;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

public class JsonPredefinedStorage extends JsonStorageBase implements IQueryableStorage<JsonRootNode> {

	private final Map<String, IDataSource<JsonRootNode>> sources;

	public JsonPredefinedStorage(File dir, String... ids) {
		Preconditions.checkArgument(dir.isDirectory());

		ImmutableMap.Builder<String, IDataSource<JsonRootNode>> builder = ImmutableMap.builder();

		for (String id : ids) {
			File f = new File(dir, id + ".json");
			builder.put(id, createFromFile(id, f));
		}

		sources = builder.build();
	}

	@Override
	public Collection<IDataSource<JsonRootNode>> listAll() {
		return sources.values();
	}

	@Override
	public IDataSource<JsonRootNode> getById(String id) {
		return sources.get(id);
	}

	@Override
	protected void removeEntry(String id) {
		// NO-OP
	}

}
