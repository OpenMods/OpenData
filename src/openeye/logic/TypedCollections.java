package openeye.logic;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

import openeye.Log;
import openeye.reports.*;
import openeye.requests.IRequest;
import openeye.requests.RequestFileInfo;
import openeye.requests.RequestPong;

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

	public static class RequestsList extends ArrayList<IRequest> {
		private static final long serialVersionUID = 4069373518963113118L;
	}

	private static final BiMap<String, Class<? extends IReport>> REPORTS_TYPES = HashBiMap.create();
	private static final BiMap<String, Class<? extends IRequest>> REQUESTS_TYPES = HashBiMap.create();

	public static final Object REPORT_LIST_CONVERTER = new TypedListConverter<IReport>(REPORTS_TYPES) {
		@Override
		protected Collection<IReport> createCollection() {
			return new ReportsList();
		}
	};

	public static final Object REQUEST_LIST_CONVERTER = new TypedListConverter<IRequest>(REQUESTS_TYPES) {
		@Override
		protected Collection<IRequest> createCollection() {
			return new RequestsList();
		}
	};

	static {
		REPORTS_TYPES.put("analytics", ReportAnalytics.class);
		REPORTS_TYPES.put("file_info", ReportFileInfo.class);
		REPORTS_TYPES.put("crash", ReportCrash.class);
		REPORTS_TYPES.put("ping", ReportPing.class);

		REQUESTS_TYPES.put("file_info", RequestFileInfo.class);
		REQUESTS_TYPES.put("pong", RequestPong.class);
	}

}
