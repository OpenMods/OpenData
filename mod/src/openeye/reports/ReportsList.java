package openeye.reports;

import java.lang.reflect.Type;
import java.util.List;

import openeye.Log;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.gson.*;

public class ReportsList {

	private static final BiMap<String, Class<?>> CLASSES = HashBiMap.create();

	static {
		CLASSES.put("analytics", ReportAnalytics.class);
		CLASSES.put("file_info", ReportFileInfo.class);
		CLASSES.put("crash", ReportCrash.class);
	}

	public List<Object> reports = Lists.newArrayList();

	public void append(Object report) {
		reports.add(report);
	}

	public static final JsonSerializer<ReportsList> SERIALIZER = new JsonSerializer<ReportsList>() {
		@Override
		public JsonElement serialize(ReportsList src, Type typeOfSrc, JsonSerializationContext context) {
			JsonArray result = new JsonArray();

			for (Object report : src.reports) {
				String type = CLASSES.inverse().get(report.getClass());
				if (type != null) {
					JsonObject serializedReport = context.serialize(report).getAsJsonObject();
					serializedReport.addProperty("type", type);
					result.add(serializedReport);
				} else Log.warn("Trying to serialize class without mapping: %s", report.getClass());
			}

			return result;
		}
	};
}
