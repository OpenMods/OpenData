package openeye.reports;

public class ReportPing implements IReport {
	public static final String TYPE = "ping";

	@Override
	public String getType() {
		return TYPE;
	}

	public String payload = "Mary Had a Little Lamb";
}
