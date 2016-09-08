package openeye.net;

import com.google.common.base.Charsets;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import openeye.logic.GsonUtils;
import openeye.struct.TypedCollections.ReportsList;
import openeye.struct.TypedCollections.ResponseList;

public class ReportSender extends GenericSender<ReportsList, ResponseList> {

	public ReportSender(String host, String path) {
		super(host, path);
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
