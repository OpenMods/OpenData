package openeye.config;

public interface IConfigPropertyHolder {

	public Object getValue();

	public void setValue(Object value);

	public Class<?> getType();

	public String name();

	public String category();

	public String comment();

}
