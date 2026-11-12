package dev.donutpay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PaymentStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE =
            FabricLoader.getInstance().getConfigDir().resolve("donutpay-tracker.json");

    public static final List<Payment> PAYMENTS = new ArrayList<>();

    private PaymentStore() {}

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                List<Payment> loaded = GSON.fromJson(Files.readString(FILE),
                        new TypeToken<List<Payment>>() {}.getType());
                if (loaded != null) PAYMENTS.addAll(loaded);
            }
        } catch (Exception e) {
            System.err.println("[DonutPay] No se pudo leer el historial: " + e);
        }
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(PAYMENTS));
        } catch (Exception e) {
            System.err.println("[DonutPay] No se pudo guardar el historial: " + e);
        }
    }

    public static void add(Payment p) {
        PAYMENTS.add(p);
        save();
    }

    public static void clear() {
        PAYMENTS.clear();
        save();
    }

    public static double net(long from, long to) {
        double n = 0;
        for (Payment p : PAYMENTS)
            if (p.time >= from && p.time <= to) n += p.incoming ? p.amount : -p.amount;
        return n;
    }

    public static int count(long from, long to) {
        int c = 0;
        for (Payment p : PAYMENTS) if (p.time >= from && p.time <= to) c++;
        return c;
    }
}
