package openeye.logic;

import java.lang.reflect.Type;

import openeye.reports.ReportsList;
import openeye.requests.RequestsList;

import com.google.gson.*;

import cpw.mods.fml.common.versioning.ArtifactVersion;

public class GsonUtils {

	public static final JsonSerializer<ArtifactVersion> VERSION_SERIALIZER = new JsonSerializer<ArtifactVersion>() {
		@Override
		public JsonElement serialize(ArtifactVersion src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject obj = new JsonObject();
			obj.addProperty("label", src.getLabel());
			obj.addProperty("version", src.getRangeString());
			return obj;
		}
	};

	public static GsonBuilder setupCommonBuilder() {
		return new GsonBuilder()
				.registerTypeAdapter(ReportsList.class, ReportsList.SERIALIZER)
				.registerTypeAdapter(RequestsList.class, RequestsList.DESERIALIZER)
				.registerTypeAdapter(ArtifactVersion.class, VERSION_SERIALIZER);
	}

	public static final Gson NET_GSON = setupCommonBuilder().create();

	public static final Gson PRETTY_GSON = setupCommonBuilder().setPrettyPrinting().create();

}
