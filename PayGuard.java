package dev.donutpay;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pide confirmación (/dpt yes | /dpt no) antes de enviar un /pay grande. */
public final class PayGuard {
    private static final Pattern PAY = Pattern.compile(
            "^pay\\s+(\\w{3,16})\\s+\\$?([\\d.,]+)\\s*([KkMmBb]?)\\s*$");
    private static String pending = null;
    private static boolean bypass = false;

    private PayGuard() {}

    /** Devuelve false para cancelar el comando. Recibe el comando sin la "/". */
    public static boolean allow(String command) {
        if (bypass) return true;
        double threshold = Config.get().confirmThreshold;
        if (threshold <= 0) return true;
        Matcher m = PAY.matcher(command.trim());
        if (!m.matches()) return true;
        double amt = PaymentParser.amount(m.group(2), m.group(3));
        if (Double.isNaN(amt) || amt < threshold) return true;

        pending = command.trim();
        msg("¿Enviar " + Fmt.money(amt) + " a " + m.group(1) + "?  /dpt yes  o  /dpt no");
        return false;
    }

    public static void confirm(boolean yes) {
        Minecraft mc = Minecraft.getInstance();
        if (pending == null) {
            msg("No hay ningún pago pendiente.");
            return;
        }
        String cmd = pending;
        pending = null;
        if (!yes) {
            msg("Pago cancelado.");
            return;
        }
        if (mc.getConnection() == null) return;
        bypass = true;
        try {
            mc.getConnection().sendCommand(cmd);
        } finally {
            bypass = false;
        }
    }

    private static void msg(String s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(Component.literal("[DonutPay] " + s), false);
    }
}
