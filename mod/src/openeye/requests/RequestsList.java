package openeye.requests;

import java.lang.reflect.Type;
import java.util.List;

import openeye.Log;
import openeye.logic.IContext;
import openeye.reports.ReportsList;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.gson.*;

public class RequestsList {
	private static final BiMap<String, Class<? extends IRequest>> CLASSES = HashBiMap.create();

	static {
		CLASSES.put("file_info", RequestFileInfo.class);
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

	public static final JsonSerializer<RequestsList> SERIALIZER = new JsonSerializer<RequestsList>() {
		@Override
		public JsonElement serialize(RequestsList src, Type typeOfSrc, JsonSerializationContext context) {
			JsonArray result = new JsonArray();

			if (src.requests != null) {
				for (Object request : src.requests) {
					String type = CLASSES.inverse().get(request.getClass());
					if (type != null) {
						JsonObject serializedRequest = context.serialize(request).getAsJsonObject();
						serializedRequest.addProperty("type", type);
						result.add(serializedRequest);
					} else Log.warn("Trying to serialize class without mapping: %s", request.getClass());
				}
			}

			return result;
		}
	};

	public boolean isEmpty() {
		return requests == null || requests.isEmpty();
	}

	public ReportsList generateResponse(IContext context) {
		Preconditions.checkState(!isEmpty());
		List<Object> reports = Lists.newArrayList();
		for (IRequest request : requests) {
			Object report = request.createReport(context);
			if (report != null) reports.add(report);
		}

		ReportsList result = new ReportsList();
		result.reports = reports;
		return result;
	}
}
