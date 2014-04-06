package openeye.storage;

import java.util.Collection;

public interface IQueryableStorage<T> {

	public Collection<IDataSource<T>> listAll();

	public IDataSource<T> getById(String id);
}