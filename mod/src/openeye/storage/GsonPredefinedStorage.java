package openeye.storage;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;

public class GsonPredefinedStorage<T> extends GsonStorageBase<T> implements IQueryableStorage<T> {

	private final Map<String, IDataSource<T>> sources;

	public GsonPredefinedStorage(File dir, Class<? extends T> cls, Gson gson, String... ids) {
		super(cls, gson);
		Preconditions.checkArgument(dir.isDirectory());

		ImmutableMap.Builder<String, IDataSource<T>> builder = ImmutableMap.builder();

		for (String id : ids) {
			File f = new File(dir, id + ".json");
			builder.put(id, createFromFile(id, f));
		}

		sources = builder.build();
	}

	@Override
	public Collection<IDataSource<T>> listAll() {
		return sources.values();
	}

	@Override
	public IDataSource<T> getById(String id) {
		return sources.get(id);
	}

	@Override
	protected void removeEntry(String id) {
		// NO-OP
	}

}
