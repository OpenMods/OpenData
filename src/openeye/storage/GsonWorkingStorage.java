package openeye.storage;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.gson.Gson;

public class GsonWorkingStorage<T> extends GsonStorageBase<T> implements IWorkingStorage<T> {

	@Override
	protected void removeEntry(String id) {
		sources.remove(id);
	}

	private final Map<String, IDataSource<T>> sources = Maps.newHashMap();

	private final String prefix;

	private final File dir;

	public GsonWorkingStorage(File dir, String prefix, Class<? extends T> cls, Gson gson) {
		super(cls, gson, "json");
		Preconditions.checkArgument(dir.isDirectory());
		this.prefix = prefix;
		this.dir = dir;

		Pattern filePattern = Pattern.compile(prefix + "-(.+)\\.json");
		for (File file : dir.listFiles()) {
			String name = file.getName();
			Matcher m = filePattern.matcher(name);
			if (m.matches()) {
				String id = m.group(1);
				sources.put(id, createFromFile(id, file));
			}
		}
	}

	@Override
	public Collection<IDataSource<T>> listAll() {
		return ImmutableList.copyOf(sources.values());
	}

	@Override
	public IDataSource<T> getById(String id) {
		return sources.get(id);
	}

	@Override
	public IDataSource<T> createNew() {
		String idPrefix = generateId();
		int count = 0;
		String id;
		do {
			id = idPrefix + "-" + count++;
		} while (sources.containsKey(id));

		return createNew(id);
	}

	@Override
	public IDataSource<T> createNew(String id) {
		String filename = generateFilename(prefix, id);
		File file = new File(dir, filename);

		IDataSource<T> newSource = createFromFile(id, file);
		sources.put(id, newSource);
		return newSource;
	}

}
