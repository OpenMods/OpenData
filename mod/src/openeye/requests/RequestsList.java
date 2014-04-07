package openeye.requests;

import java.lang.reflect.Type;
import java.util.List;

import openeye.Log;
import openeye.reports.ReportsList;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.gson.*;

public class RequestsList {
	private static final BiMap<String, Class<? extends IRequest>> CLASSES = HashBiMap.create();

	public static void registerRequestMapping(String type, Class<? extends IRequest> cls) {
		CLASSES.put(type, cls);
	}

	public List<IRequest> requests;

	public static final JsonDeserializer<RequestsList> DESERIALIZER = new JsonDeserializer<RequestsList>() {
		@Override
		public RequestsList deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonArray requests = json.getAsJsonArray();

			List<IRequest> result = Lists.newArrayList();
			for (JsonElement e : requests) {
				try {
					JsonObject obj = e.getAsJsonObject();
					JsonElement type = obj.get("type");
					String typeId = type.getAsString();

					Class<? extends IRequest> cls = CLASSES.get(typeId);
					if (cls != null) {
						IRequest request = context.deserialize(obj, cls);
						result.add(request);
					} else Log.warn("Invalid request type: %s", typeId);
				} catch (Throwable t) {
					Log.warn(t, "Failed to deserialize request %s", e);
				}
			}

			RequestsList list = new RequestsList();
			list.requests = result;
			return list;
		}
	};

	public boolean isEmpty() {
		return requests == null || requests.isEmpty();
	}

	public ReportsList generateResponse() {
		Preconditions.checkState(!isEmpty());
		List<Object> reports = Lists.newArrayList();
		for (IRequest request : requests) {
			Object report = request.createReport();
			reports.add(report);
		}

		ReportsList result = new ReportsList();
		result.reports = reports;
		return result;
	}
}
