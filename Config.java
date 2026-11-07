package pl.tpahere.auto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("tpahere_auto.json");

    public static boolean enabled = false;
    public static int delaySeconds = 3;
    public static int cooldownMinutes = 10;

    private static class Data {
        boolean enabled = false;
        int delaySeconds = 3;
        int cooldownMinutes = 10;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                try (Reader r = Files.newBufferedReader(FILE)) {
                    Data d = GSON.fromJson(r, Data.class);
                    if (d != null) {
                        enabled = d.enabled;
                        delaySeconds = Math.max(1, Math.min(60, d.delaySeconds));
                        cooldownMinutes = Math.max(1, Math.min(120, d.cooldownMinutes));
                    }
                }
            }
        } catch (Exception e) {
            TpaHereClient.LOGGER.warn("Nie udalo sie wczytac configu", e);
        }
    }

    public static void save() {
        try {
            Data d = new Data();
            d.enabled = enabled;
            d.delaySeconds = delaySeconds;
            d.cooldownMinutes = cooldownMinutes;
            try (Writer w = Files.newBufferedWriter(FILE)) {
                GSON.toJson(d, w);
            }
        } catch (Exception e) {
            TpaHereClient.LOGGER.warn("Nie udalo sie zapisac configu", e);
        }
    }
}
