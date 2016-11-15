package myschedule.rest.util;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Gson serializer for the generic type Class used in JobInfo.java
 *
 */
public class JobDetailSerializer implements JsonSerializer<Class> {

	@Override
	public JsonElement serialize(Class arg0, Type arg1, JsonSerializationContext arg2) {
		return new JsonPrimitive(arg0.getCanonicalName());
    }
	
}
