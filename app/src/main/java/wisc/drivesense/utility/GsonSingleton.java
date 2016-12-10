package wisc.drivesense.utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Created by Alex Sherman on 11/10/2016.
 */

public class GsonSingleton implements JsonDeserializer<TraceMessage> {
    private static Gson _gson;

    public static final Map<String, Type> typeLookup;
    static
    {
        typeLookup = new HashMap<String, Type>();

        typeLookup.put(Trace.GPS.class.getSimpleName(), Trace.GPS.class);
        typeLookup.put(Trace.Trip.class.getSimpleName(), Trace.Trip.class);
        typeLookup.put(Trace.Accel.class.getSimpleName(), Trace.Accel.class);
        typeLookup.put(Trace.Gyro.class.getSimpleName(), Trace.Gyro.class);
        typeLookup.put(Trace.Rotation.class.getSimpleName(), Trace.Rotation.class);
        typeLookup.put(Trace.Magnetometer.class.getSimpleName(), Trace.Magnetometer.class);
    }
    public static final Map<Type, String> typeNameLookup;
    static {
        typeNameLookup = new HashMap<Type, String>();
        for (Map.Entry<String, Type> entry: typeLookup.entrySet()) {
            typeNameLookup.put(entry.getValue(), entry.getKey());
        }
    }

    public static Gson gson() {
        if(_gson == null) {
            GsonBuilder b = new GsonBuilder();
            b.registerTypeAdapter(TraceMessage.class, new GsonSingleton());
            _gson = b.create();
        }
        return _gson;
    }

    public static String toJson(Object o) {
        return gson().toJson(o);
    }
    public static Object fromJson(String j, Type t) {
        return gson().fromJson(j, t);
    }
    public static <T> T fromJson(String j, Class<T> t) {
        return gson().fromJson(j, t);
    }

    @Override
    public TraceMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jobj = json.getAsJsonObject();
        String type = jobj.get("type").getAsString();
        return new TraceMessage((Trace)context.deserialize(jobj.get("value"), typeLookup.get(type)));
    }
}
