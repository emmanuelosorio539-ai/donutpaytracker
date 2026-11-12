package dev.donutpay;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class DonutPayClient implements ClientModInitializer {
    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        Config.load();
        PaymentStore.load();

        // Solo mensajes del servidor (no chat de jugadores) para que nadie falsifique pagos.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) PaymentParser.handle(message.getString());
        });

        // Confirmación de /pay grandes.
        ClientSendMessageEvents.ALLOW_COMMAND.register(PayGuard::allow);

        // HUD.
        HudRenderCallback.EVENT.register(DonutPayHud::render);

        // Tecla para abrir la ventana (cámbiala en Controles; por defecto G).
        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.donutpay.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) mc.setScreen(new TrackerScreen());
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("dpt")
                        .executes(c -> {
                            Minecraft mc = Minecraft.getInstance();
                            mc.execute(() -> mc.setScreen(new TrackerScreen()));
                            return 1;
                        })
                        .then(literal("yes").executes(c -> { PayGuard.confirm(true); return 1; }))
                        .then(literal("no").executes(c -> { PayGuard.confirm(false); return 1; }))
                        .then(literal("hud").executes(c -> {
                            Config cfg = Config.get();
                            cfg.hud = !cfg.hud;
                            Config.save();
                            return say(c, "HUD " + (cfg.hud ? "activado" : "desactivado"));
                        }))
                        .then(literal("clear").executes(c -> {
                            PaymentStore.clear();
                            return say(c, "Historial borrado.");
                        }))
                        .then(literal("confirm").then(argument("monto", StringArgumentType.word()).executes(c -> {
                            String raw = StringArgumentType.getString(c, "monto");
                            double v = raw.equalsIgnoreCase("off") ? 0 : PaymentParser.parseLoose(raw);
                            if (Double.isNaN(v)) return say(c, "Monto inválido. Ej: /dpt confirm 500k  o  /dpt confirm off");
                            Config.get().confirmThreshold = v;
                            Config.save();
                            return say(c, v <= 0 ? "Confirmación desactivada."
                                    : "Se pedirá confirmación desde " + Fmt.money(v));
                        })))
                        .then(literal("rakeback").then(argument("porcentaje", DoubleArgumentType.doubleArg(0, 100)).executes(c -> {
                            Config.get().rakeback = DoubleArgumentType.getDouble(c, "porcentaje");
                            Config.save();
                            return say(c, "Rakeback: " + Config.get().rakeback + "%");
                        })))
                        .then(literal("session")
                                .then(literal("start").executes(c -> {
                                    Config.get().sessionStart = System.currentTimeMillis();
                                    Config.save();
                                    return say(c, "Sesión iniciada.");
                                }))
                                .then(literal("stop").executes(c -> {
                                    Config cfg = Config.get();
                                    if (cfg.sessionStart <= 0) return say(c, "No hay sesión activa.");
                                    long now = System.currentTimeMillis();
                                    double net = PaymentStore.net(cfg.sessionStart, now);
                                    cfg.sessions.add(new long[]{cfg.sessionStart, now});
                                    cfg.sessionStart = 0;
                                    Config.save();
                                    return say(c, "Sesión terminada. Neto: " + Fmt.money(net));
                                })))
                        .then(literal("bid")
                                .then(literal("start").executes(c -> {
                                    BidManager.start();
                                    return say(c, "Bid iniciada. Cada jugador cuenta con su pago más alto (no se suman).");
                                }))
                                .then(literal("stop").executes(c -> {
                                    BidManager.stop();
                                    Map.Entry<String, Double> top = BidManager.top();
                                    return say(c, top == null ? "Bid terminada sin pujas."
                                            : "Bid terminada. Gana " + top.getKey() + " con " + Fmt.money(top.getValue()));
                                }))
                                .then(literal("top").executes(c -> {
                                    Map.Entry<String, Double> top = BidManager.top();
                                    return say(c, top == null ? "Sin pujas todavía."
                                            : "Top bid: " + top.getKey() + " con " + Fmt.money(top.getValue()));
                                })))));
    }

    private static int say(CommandContext<FabricClientCommandSource> c, String s) {
        c.getSource().sendFeedback(Component.literal("[DonutPay] " + s));
        return 1;
    }
}
