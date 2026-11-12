package dev.donutpay;

/** Un pago individual. Nunca se fusiona con otros: cada /pay es una entrada. */
public class Payment {
    public long time;
    public boolean incoming;
    public String player;
    public double amount;

    public Payment() {}

    public Payment(long time, boolean incoming, String player, double amount) {
        this.time = time;
        this.incoming = incoming;
        this.player = player;
        this.amount = amount;
    }
}
