package openeye.struct;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

import openeye.Log;
import openeye.reports.*;
import openeye.responses.*;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.*;

public class TypedCollections {

	private abstract static class TypedListConverter<T> implements JsonSerializer<Collection<T>>, JsonDeserializer<Collection<T>> {
		private final BiMap<String, Class<? extends T>> mapping;

		private TypedListConverter(BiMap<String, Class<? extends T>> mapping) {
			this.mapping = mapping;
		}

		@Override
		public JsonElement serialize(Collection<T> src, Type typeOfSrc, JsonSerializationContext context) {
			JsonArray result = new JsonArray();

			for (T entry : src) {
				final Class<? extends Object> entryClass = entry.getClass();
				final String type = mapping.inverse().get(entryClass);
				if (type != null) {
					JsonObject serializedReport = context.serialize(entry).getAsJsonObject();
					serializedReport.addProperty("type", type);
					result.add(serializedReport);
				} else Log.warn("Trying to serialize class without mapping: %s", entryClass);
			}

			return result;
		}

		@Override
		public Collection<T> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonArray requests = json.getAsJsonArray();

			Collection<T> result = createCollection();
			for (JsonElement e : requests) {
				try {
					JsonObject obj = e.getAsJsonObject();
					JsonElement type = obj.get("type");
					String typeId = type.getAsString();

					Class<? extends T> cls = mapping.get(typeId);
					if (cls != null) {
						T request = context.deserialize(obj, cls);
						result.add(request);
					} else Log.warn("Invalid request type: %s", typeId);
				} catch (Throwable t) {
					Log.warn(t, "Failed to deserialize request %s", e);
				}
			}

			return result;
		}

		protected abstract Collection<T> createCollection();
	}

	public static class ReportsList extends ArrayList<IReport> {
		private static final long serialVersionUID = -6580030458427773185L;
	}

	public static class ResponseList extends ArrayList<IResponse> {
		private static final long serialVersionUID = 4069373518963113118L;
	}

	private static final BiMap<String, Class<? extends IReport>> REPORTS_TYPES = HashBiMap.create();
	private static final BiMap<String, Class<? extends IResponse>> RESPONSE_TYPES = HashBiMap.create();

	public static final Object REPORT_LIST_CONVERTER = new TypedListConverter<IReport>(REPORTS_TYPES) {
		@Override
		protected Collection<IReport> createCollection() {
			return new ReportsList();
		}
	};

	public static final Object RESPONSE_LIST_CONVERTER = new TypedListConverter<IResponse>(RESPONSE_TYPES) {
		@Override
		protected Collection<IResponse> createCollection() {
			return new ResponseList();
		}
	};

	static {
		REPORTS_TYPES.put(ReportAnalytics.TYPE, ReportAnalytics.class);
		REPORTS_TYPES.put(ReportFileInfo.TYPE, ReportFileInfo.class);
		REPORTS_TYPES.put(ReportCrash.TYPE, ReportCrash.class);
		REPORTS_TYPES.put(ReportPing.TYPE, ReportPing.class);
		REPORTS_TYPES.put(ReportKnownFiles.TYPE, ReportKnownFiles.class);
		REPORTS_TYPES.put(ReportFileContents.TYPE, ReportFileContents.class);

		RESPONSE_TYPES.put(ResponseFileInfo.TYPE, ResponseFileInfo.class);
		RESPONSE_TYPES.put(ResponsePong.TYPE, ResponsePong.class);
		RESPONSE_TYPES.put(ResponseFileContents.TYPE, ResponseFileContents.class);
		RESPONSE_TYPES.put(ResponseDangerousFile.TYPE, ResponseDangerousFile.class);
		RESPONSE_TYPES.put(ResponseModMsg.TYPE, ResponseModMsg.class);
		RESPONSE_TYPES.put(ResponseError.TYPE, ResponseError.class);
		RESPONSE_TYPES.put(ResponseKnownCrash.TYPE, ResponseKnownCrash.class);
	}

}
