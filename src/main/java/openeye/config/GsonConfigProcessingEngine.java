package openeye.config;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Table;
import com.google.common.io.Closer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import openeye.Log;

public class GsonConfigProcessingEngine implements IConfigProcessingEngine {

	private static final String VALUE_TAG = "value";
	private static final String COMMENT_TAG = "comment";

	private static JsonElement parse(File file) {
		try {
			InputStream stream = new FileInputStream(file);
			try {
				Reader fileReader = new InputStreamReader(stream, Charsets.UTF_8);
				JsonReader jsonReader = new JsonReader(fileReader);
				jsonReader.setLenient(true);
				return Streams.parse(jsonReader);
			} finally {
				stream.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void write(File file, JsonElement element) {
		try {
			Closer closer = Closer.create();
			try {
				FileOutputStream stream = closer.register(new FileOutputStream(file));
				Writer fileWriter = closer.register(new OutputStreamWriter(stream, Charsets.UTF_8));
				JsonWriter jsonWriter = closer.register(new JsonWriter(fileWriter));
				jsonWriter.setIndent("    ");
				Streams.write(element, jsonWriter);
			} finally {
				closer.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static JsonElement dumpConfig(Table<String, String, IConfigPropertyHolder> properties) {
		JsonObject result = new JsonObject();

		Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();

		for (Map.Entry<String, Map<String, IConfigPropertyHolder>> category : properties.rowMap().entrySet()) {
			JsonObject categoryNode = new JsonObject();

			for (Map.Entry<String, IConfigPropertyHolder> property : category.getValue().entrySet()) {
				JsonObject propertyNode = new JsonObject();

				IConfigPropertyHolder propertyHolder = property.getValue();

				final String comment = propertyHolder.comment();
				if (!Strings.isNullOrEmpty(comment)) propertyNode.addProperty(COMMENT_TAG, comment);

				try {
					Object value = propertyHolder.getValue();
					JsonElement serialized = gson.toJsonTree(value);
					propertyNode.add(VALUE_TAG, serialized);
				} catch (Exception e) {
					Log.warn(e, "Failed to serialize property %s:%s", propertyHolder.category(), propertyHolder.name());
				}

				categoryNode.add(property.getKey(), propertyNode);
			}

			result.add(category.getKey(), categoryNode);
		}

		return result;
	}

	private static boolean loadConfig(JsonElement parsed, Table<String, String, IConfigPropertyHolder> properties) {
		if (!parsed.isJsonObject()) return true;

		Gson gson = new Gson();

		JsonObject rootNode = parsed.getAsJsonObject();

		boolean missingFields = false;

		for (Map.Entry<String, Map<String, IConfigPropertyHolder>> e : properties.rowMap().entrySet()) {
			JsonElement categoryTmp = rootNode.get(e.getKey());
			missingFields |= parseCategory(categoryTmp, e.getValue(), gson);
		}

		return missingFields;
	}

	private static boolean parseCategory(JsonElement categoryElement, Map<String, IConfigPropertyHolder> properties, Gson gson) {
		if (!(categoryElement instanceof JsonObject)) return true;

		JsonObject categoryNode = categoryElement.getAsJsonObject();

		boolean missingFields = false;
		for (Map.Entry<String, IConfigPropertyHolder> e : properties.entrySet()) {
			JsonElement propertyValue = categoryNode.get(e.getKey());
			missingFields |= parseProperty(propertyValue, e.getValue(), gson);
		}

		return missingFields;
	}

	private static boolean parseProperty(JsonElement propertyElement, IConfigPropertyHolder property, Gson gson) {
		if (!(propertyElement instanceof JsonObject)) return true;

		JsonObject propertyNode = propertyElement.getAsJsonObject();

		JsonElement value = propertyNode.get(VALUE_TAG);

		if (value == null) return true;

		try {
			Object parsedValue = gson.fromJson(value, property.getType());
			property.setValue(parsedValue);
		} catch (Exception e) {
			Log.warn(e, "Failed to parse value of field %s:%s", property.category(), property.name());
			return true;
		}

		JsonElement comment = propertyNode.get(COMMENT_TAG);

		final String expectedComment = property.comment();

		if (comment == null) {
			return !Strings.isNullOrEmpty(expectedComment);
		} else if (comment.isJsonPrimitive()) {
			String commentValue = comment.getAsString();
			return !expectedComment.equals(commentValue);
		}

		return true;
	}

	@Override
	public boolean loadConfig(File source, Table<String, String, IConfigPropertyHolder> properties) {
		if (source.exists()) {
			try {
				JsonElement parsed = parse(source);
				return loadConfig(parsed, properties);
			} catch (Exception e) {
				Log.warn(e, "Failed to parse file %s, using defaults", source);
			}
		}
		return true;
	}

	@Override
	public void dumpConfig(File source, Table<String, String, IConfigPropertyHolder> properties) {
		try {
			JsonElement serialized = dumpConfig(properties);
			write(source, serialized);
		} catch (Exception e) {
			Log.warn(e, "Failed to save config to file %s", source);
		}
	}

}
