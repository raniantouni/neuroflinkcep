package core.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class JSONSingleton {
    private static Gson GSON_INSTANCE = null;

    //Prevent instantiation
    private JSONSingleton() {
    }

    private static Gson buildGson() {
        return new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .create();
    }

    public static String toJson(Object obj) {
        return JSONSingleton.getGSON().toJson(obj);
    }

    public static <T> T fromJson(String str, Class<T> classOfT) {
        return JSONSingleton.getGSON().fromJson(str, classOfT);
    }

    private static Gson getGSON() {
        if (GSON_INSTANCE == null) {
            GSON_INSTANCE = buildGson();
        }
        return GSON_INSTANCE;
    }
}
