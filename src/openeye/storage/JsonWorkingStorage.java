package openeye.storage;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import argo.jdom.JsonRootNode;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class JsonWorkingStorage extends JsonStorageBase implements IWorkingStorage<JsonRootNode> {

	@Override
	protected void removeEntry(String id) {
		sources.remove(id);
	}

	private final Map<String, IDataSource<JsonRootNode>> sources = Maps.newHashMap();

	private final String prefix;

	private final File dir;

	public JsonWorkingStorage(File dir, String prefix) {
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
	public Collection<IDataSource<JsonRootNode>> listAll() {
		return sources.values();
	}

	@Override
	public IDataSource<JsonRootNode> getById(String id) {
		return sources.get(id);
	}

	@Override
	public IDataSource<JsonRootNode> createNew() {
		String id;
		do {
			id = generateId();
		} while (sources.containsKey(id));

		return createNew(id);
	}

	@Override
	public IDataSource<JsonRootNode> createNew(String id) {
		String filename = generateFilename(prefix, id);
		File file = new File(dir, filename);

		IDataSource<JsonRootNode> newSource = createFromFile(id, file);
		sources.put(id, newSource);
		return newSource;
	}

}
