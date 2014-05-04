package openeye.net;

import java.io.*;
import java.net.URL;

import openeye.logic.GsonUtils;
import openeye.logic.TypedCollections.ReportsList;
import openeye.logic.TypedCollections.ResponseList;

import com.google.common.base.Charsets;

public class ReportSender extends GenericSender<ReportsList, ResponseList> {

	public ReportSender(URL url) {
		super(url);
	}

	public ReportSender(String url) {
		super(url);
	}

	@Override
	protected void encodeRequest(OutputStream output, ReportsList request) throws IOException {
		Writer writer = new OutputStreamWriter(output, Charsets.UTF_8);
		GsonUtils.NET_GSON.toJson(request, writer);
		writer.close();
	}

	@Override
	protected ResponseList decodeResponse(InputStream input) throws IOException {
		Reader reader = new InputStreamReader(input, Charsets.UTF_8);
		ResponseList result = GsonUtils.NET_GSON.fromJson(reader, ResponseList.class);
		reader.close();
		return result;
	}

}
