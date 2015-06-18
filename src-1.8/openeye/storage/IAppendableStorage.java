package openeye.storage;

public interface IAppendableStorage<T> {
	public IDataSource<T> createNew();

	public IDataSource<T> createNew(String id);
}