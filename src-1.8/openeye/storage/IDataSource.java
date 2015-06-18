package openeye.storage;

public interface IDataSource<T> {
	public String getId();

	public T retrieve();

	public void store(T value);

	public void delete();
}
