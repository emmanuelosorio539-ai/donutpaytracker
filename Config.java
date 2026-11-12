package dev.donutpay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("donutpay-config.json");
    private static Config instance = new Config();

    public boolean hud = false;            // apagado por defecto
    public double confirmThreshold = 0;    // 0 = sin confirmación
    public double rakeback = 0;            // porcentaje
    public long sessionStart = 0;          // 0 = sin sesión activa
    public List<long[]> sessions = new ArrayList<>(); // {inicio, fin}

    public static Config get() { return instance; }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                Config c = GSON.fromJson(Files.readString(FILE), Config.class);
                if (c != null) {
                    if (c.sessions == null) c.sessions = new ArrayList<>();
                    instance = c;
                }
            }
        } catch (Exception e) {
            System.err.println("[DonutPay] No se pudo leer la config: " + e);
        }
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(instance));
        } catch (Exception e) {
            System.err.println("[DonutPay] No se pudo guardar la config: " + e);
        }
    }
}
