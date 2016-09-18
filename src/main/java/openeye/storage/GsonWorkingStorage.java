package openeye.storage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GsonWorkingStorage<T> extends GsonDirStorage<T> implements IWorkingStorage<T> {

	private final Map<String, IDataSource<T>> sources = Maps.newHashMap();

	public GsonWorkingStorage(File dir, String prefix, Class<? extends T> cls, Gson gson) {
		super(dir, prefix, cls, gson, "json");

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
	protected synchronized void removeEntry(String id) {
		sources.remove(id);
	}

	@Override
	public synchronized Collection<IDataSource<T>> listAll() {
		return ImmutableList.copyOf(sources.values());
	}

	@Override
	public synchronized IDataSource<T> getById(String id) {
		return sources.get(id);
	}

	@Override
	public synchronized IDataSource<T> createNew() {
		String idPrefix = generateId();
		int count = 0;
		String id;
		do {
			id = idPrefix + "-" + count++;
		} while (sources.containsKey(id));

		return createNew(id);
	}

	@Override
	public synchronized IDataSource<T> createNew(String id) {
		IDataSource<T> newSource = super.createNew(id);
		sources.put(id, newSource);
		return newSource;
	}

}
