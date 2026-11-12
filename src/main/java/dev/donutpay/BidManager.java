package dev.donutpay;

import java.util.*;

/**
 * Bids / subastas. Cada pago recibido es una puja INDEPENDIENTE.
 * Los pagos del mismo jugador NO se suman: su puja válida es su pago individual más alto.
 */
public final class BidManager {
    public static boolean active = false;
    public static final List<Payment> BIDS = new ArrayList<>();

    private BidManager() {}

    public static void start() {
        BIDS.clear();
        active = true;
    }

    public static void stop() {
        active = false;
    }

    public static void onPayment(Payment p) {
        if (active && p.incoming) BIDS.add(p);
    }

    /** Mejor puja individual por jugador (máximo, nunca suma). */
    public static Map<String, Double> best() {
        Map<String, Double> best = new LinkedHashMap<>();
        for (Payment b : BIDS) best.merge(b.player, b.amount, Math::max);
        return best;
    }

    public static Map.Entry<String, Double> top() {
        return best().entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
    }
}
