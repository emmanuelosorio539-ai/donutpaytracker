package dev.donutpay;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.text.SimpleDateFormat;
import java.util.*;

public class TrackerScreen extends Screen {
    private static final int GREEN = 0xFF55FF55, RED = 0xFFFF5555, WHITE = 0xFFFFFFFF,
            GRAY = 0xFFAAAAAA, YELLOW = 0xFFFFFF55;
    private static final int ROW_H = 12, HEADER_Y = 72, TOP = 86, BOTTOM_MARGIN = 20;
    private static final SimpleDateFormat TIME = new SimpleDateFormat("dd/MM HH:mm:ss");

    private int tab = 0; // 0 historial, 1 jugadores, 2 bids, 3 sesiones
    private int scroll = 0;

    public TrackerScreen() {
        super(Component.literal("DonutPay Tracker"));
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 160;
        String[] names = {"Historial", "Jugadores", "Bids", "Sesiones"};
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            addRenderableWidget(Button.builder(Component.literal(names[i]), b -> { tab = idx; scroll = 0; })
                    .bounds(x + i * 66, 20, 62, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Borrar"), b -> { PaymentStore.clear(); scroll = 0; })
                .bounds(x + 4 * 66, 20, 56, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);

        Config cfg = Config.get();
        int left = this.width / 2 - 160;
        double total = PaymentStore.net(0, Long.MAX_VALUE);
        g.drawString(this.font, "Neto total: " + Fmt.money(total), left, 46, total >= 0 ? GREEN : RED);
        if (cfg.sessionStart > 0) {
            double s = PaymentStore.net(cfg.sessionStart, Long.MAX_VALUE);
            g.drawString(this.font, "Sesión: " + Fmt.money(s), left + 160, 46, s >= 0 ? GREEN : RED);
        }
        g.drawString(this.font, PaymentStore.PAYMENTS.size() + " pagos", left, 58, GRAY);

        int visible = Math.max(1, (this.height - TOP - BOTTOM_MARGIN) / ROW_H);
        switch (tab) {
            case 0 -> drawHistory(g, left, visible);
            case 1 -> drawPlayers(g, left, visible);
            case 2 -> drawBids(g, left, visible);
            default -> drawSessions(g, left, visible);
        }
    }

    private int clampScroll(int size, int visible) {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, size - visible)));
        return scroll;
    }

    private void drawHistory(GuiGraphics g, int left, int visible) {
        g.drawString(this.font, "Cada pago es una entrada (sin apilar)", left, HEADER_Y, GRAY);
        List<Payment> list = new ArrayList<>(PaymentStore.PAYMENTS);
        Collections.reverse(list);
        int s = clampScroll(list.size(), visible);
        for (int i = 0; i < visible && i + s < list.size(); i++) {
            Payment p = list.get(i + s);
            int y = TOP + i * ROW_H;
            g.drawString(this.font, TIME.format(new Date(p.time)), left, y, GRAY);
            g.drawString(this.font, (p.incoming ? "+ " : "- ") + p.player, left + 90, y, WHITE);
            g.drawString(this.font, Fmt.money(p.amount), left + 240, y, p.incoming ? GREEN : RED);
        }
    }

    private void drawPlayers(GuiGraphics g, int left, int visible) {
        Config cfg = Config.get();
        g.drawString(this.font, "Jugador", left, HEADER_Y, GRAY);
        g.drawString(this.font, "Neto", left + 130, HEADER_Y, GRAY);
        g.drawString(this.font, "Rakeback (" + cfg.rakeback + "%)", left + 220, HEADER_Y, GRAY);

        Map<String, double[]> m = new HashMap<>(); // {neto, total recibido}
        for (Payment p : PaymentStore.PAYMENTS) {
            if (p.time < cfg.sessionStart) continue;
            double[] a = m.computeIfAbsent(p.player, k -> new double[2]);
            a[0] += p.incoming ? p.amount : -p.amount;
            if (p.incoming) a[1] += p.amount;
        }
        List<Map.Entry<String, double[]>> rows = new ArrayList<>(m.entrySet());
        rows.sort((a, b) -> Double.compare(Math.abs(b.getValue()[0]), Math.abs(a.getValue()[0])));
        int s = clampScroll(rows.size(), visible);
        for (int i = 0; i < visible && i + s < rows.size(); i++) {
            Map.Entry<String, double[]> e = rows.get(i + s);
            int y = TOP + i * ROW_H;
            double net = e.getValue()[0];
            g.drawString(this.font, e.getKey(), left, y, WHITE);
            g.drawString(this.font, Fmt.money(net), left + 130, y, net >= 0 ? GREEN : RED);
            g.drawString(this.font, Fmt.money(e.getValue()[1] * cfg.rakeback / 100.0), left + 220, y, YELLOW);
        }
    }

    private void drawBids(GuiGraphics g, int left, int visible) {
        g.drawString(this.font, (BidManager.active ? "Bid ACTIVA" : "Bid detenida")
                + " - cuenta el pago más alto de cada jugador, no la suma", left, HEADER_Y,
                BidManager.active ? GREEN : GRAY);

        Map<String, Double> best = BidManager.best();
        Map<String, Integer> counts = new HashMap<>();
        for (Payment b : BidManager.BIDS) counts.merge(b.player, 1, Integer::sum);
        List<Map.Entry<String, Double>> rows = new ArrayList<>(best.entrySet());
        rows.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        int s = clampScroll(rows.size(), visible);
        for (int i = 0; i < visible && i + s < rows.size(); i++) {
            Map.Entry<String, Double> e = rows.get(i + s);
            int y = TOP + i * ROW_H;
            g.drawString(this.font, (i + s + 1) + ". " + e.getKey(), left, y, i + s == 0 ? YELLOW : WHITE);
            g.drawString(this.font, Fmt.money(e.getValue()), left + 130, y, GREEN);
            g.drawString(this.font, counts.getOrDefault(e.getKey(), 0) + " pagos", left + 220, y, GRAY);
        }
    }

    private void drawSessions(GuiGraphics g, int left, int visible) {
        Config cfg = Config.get();
        g.drawString(this.font, "Sesiones (/dpt session start | stop)", left, HEADER_Y, GRAY);
        List<long[]> list = new ArrayList<>(cfg.sessions);
        Collections.reverse(list);
        int s = clampScroll(list.size(), visible);
        for (int i = 0; i < visible && i + s < list.size(); i++) {
            long[] se = list.get(i + s);
            int y = TOP + i * ROW_H;
            double net = PaymentStore.net(se[0], se[1]);
            g.drawString(this.font, TIME.format(new Date(se[0])), left, y, GRAY);
            g.drawString(this.font, Fmt.money(net), left + 110, y, net >= 0 ? GREEN : RED);
            g.drawString(this.font, PaymentStore.count(se[0], se[1]) + " pagos", left + 220, y, GRAY);
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        scroll -= (int) Math.signum(vertical) * 3;
        if (scroll < 0) scroll = 0;
        return true;
    }
}
