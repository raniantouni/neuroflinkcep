package web.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Random;

public interface RepositoryDocument<T> {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    Random random = new Random(0);

    String id();

    T getObject();
}
