package openeye.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import openeye.struct.TypedCollections;
import openeye.struct.TypedCollections.ReportsList;
import openeye.struct.TypedCollections.ResponseList;

public class GsonUtils {

	public static final JsonSerializer<ArtifactVersion> VERSION_SERIALIZER = (src, typeOfSrc, context) -> {
		JsonObject obj = new JsonObject();
		obj.addProperty("label", src.getLabel());
		obj.addProperty("version", src.getRangeString());
		return obj;
	};

	public static GsonBuilder setupCommonBuilder() {
		return new GsonBuilder()
				.registerTypeAdapter(ReportsList.class, TypedCollections.REPORT_LIST_CONVERTER)
				.registerTypeAdapter(ResponseList.class, TypedCollections.RESPONSE_LIST_CONVERTER)
				.registerTypeAdapter(ArtifactVersion.class, VERSION_SERIALIZER);
	}

	public static final Gson NET_GSON = setupCommonBuilder().create();

	public static final Gson PRETTY_GSON = setupCommonBuilder().setPrettyPrinting().create();

}
