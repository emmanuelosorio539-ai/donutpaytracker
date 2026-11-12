package dev.donutpay;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DonutPayHud {
    private DonutPayHud() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Config c = Config.get();
        if (!c.hud) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;

        List<String> lines = new ArrayList<>();
        long from = c.sessionStart > 0 ? c.sessionStart : 0;
        lines.add((c.sessionStart > 0 ? "Sesión: " : "Total: ")
                + Fmt.money(PaymentStore.net(from, Long.MAX_VALUE)));
        if (BidManager.active) {
            Map.Entry<String, Double> top = BidManager.top();
            lines.add(top == null ? "Bid: sin pujas" : "Top bid: " + top.getKey() + " " + Fmt.money(top.getValue()));
        }

        int w = 0;
        for (String s : lines) w = Math.max(w, mc.font.width(s));
        g.fill(4, 4, 4 + w + 6, 4 + lines.size() * 10 + 4, 0x90000000);
        int y = 6;
        for (String s : lines) {
            g.drawString(mc.font, s, 7, y, 0xFFFFFFFF);
            y += 10;
        }
    }
}
