package dev.donutpay;

public final class Fmt {
    private Fmt() {}

    public static String money(double v) {
        double a = Math.abs(v);
        String s;
        if (a >= 1e9) s = String.format("%.2fB", a / 1e9);
        else if (a >= 1e6) s = String.format("%.2fM", a / 1e6);
        else if (a >= 1e3) s = String.format("%.2fK", a / 1e3);
        else s = String.format("%.0f", a);
        return (v < 0 ? "-$" : "$") + s;
    }
}
