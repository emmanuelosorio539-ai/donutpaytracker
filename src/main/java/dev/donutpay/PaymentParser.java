package dev.donutpay;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PaymentParser {
    // ⚠ PLACEHOLDERS: ajústalos con las líneas reales del chat de DonutSMP.
    // Grupo 1 = jugador, grupo 2 = número, grupo 3 = sufijo (K/M/B).
    static final Pattern INCOMING = Pattern.compile(
            "(\\w{3,16}) (?:paid|sent) you \\$?([\\d.,]+)\\s*([KkMmBb]?)");
    static final Pattern OUTGOING = Pattern.compile(
            "You (?:paid|sent) (\\w{3,16}) \\$?([\\d.,]+)\\s*([KkMmBb]?)");
    private static final Pattern LOOSE = Pattern.compile("^\\$?([\\d.,]+)\\s*([KkMmBb]?)$");

    private PaymentParser() {}

    public static void handle(String line) {
        Matcher in = INCOMING.matcher(line);
        if (in.find()) {
            add(true, in);
            return;
        }
        Matcher out = OUTGOING.matcher(line);
        if (out.find()) add(false, out);
    }

    private static void add(boolean incoming, Matcher m) {
        double value = amount(m.group(2), m.group(3));
        if (Double.isNaN(value)) return;
        Payment p = new Payment(System.currentTimeMillis(), incoming, m.group(1), value);
        PaymentStore.add(p);
        BidManager.onPayment(p);
    }

    /** "1,5" + "M" -> 1500000. Devuelve NaN si no es un número válido. */
    public static double amount(String num, String suffix) {
        try {
            double v = Double.parseDouble(num.replace(",", ""));
            switch (suffix.toUpperCase()) {
                case "K" -> v *= 1_000;
                case "M" -> v *= 1_000_000;
                case "B" -> v *= 1_000_000_000;
                default -> {}
            }
            return v;
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /** Acepta cosas como "500k", "$1.5m", "2000". */
    public static double parseLoose(String s) {
        Matcher m = LOOSE.matcher(s.trim());
        return m.matches() ? amount(m.group(1), m.group(2)) : Double.NaN;
    }
}
