package openeye.net;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import openeye.logic.GsonUtils;
import openeye.reports.ReportsList;
import openeye.requests.RequestsList;

public class ReportSender extends GenericSender<ReportsList, RequestsList> {

	public ReportSender(URL url) {
		super(url);
	}

	public ReportSender(String url) {
		super(url);
	}

	@Override
	protected void encodeRequest(OutputStream output, ReportsList request) throws IOException {
		Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
		GsonUtils.NET_GSON.toJson(request, writer);
		writer.close();
	}

	@Override
	protected RequestsList decodeResponse(InputStream input) throws IOException {
		Reader reader = new InputStreamReader(input);
		RequestsList result = GsonUtils.NET_GSON.fromJson(reader, RequestsList.class);
		reader.close();
		return result;
	}

}
