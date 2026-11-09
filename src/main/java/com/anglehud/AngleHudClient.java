package com.anglehud;

import com.anglehud.mixin.ScreenInvoker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AngleHudClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("anglehud");
    private static boolean openMenu = false;

    @Override
    public void onInitializeClient() {
        AngleConfig.load();

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("anglehud", "angle_display"), (graphics, tickCounter) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) return;
            if (mc.screen instanceof AngleScreens.Base) return;
            AngleRenderer.drawHud(graphics, mc);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            for (String name : new String[]{"winkel", "angle"}) {
                dispatcher.register(ClientCommandManager.literal(name).executes(ctx -> {
                    openMenu = true;
                    return 1;
                }));
            }
        });

        // Menü erst im nächsten Tick öffnen, sonst schließt der Chat es wieder.
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (openMenu) {
                openMenu = false;
                mc.setScreen(new AngleScreens.MainMenu(null));
            }
        });

        // Chat-Nachrichten für den Übersetzer merken
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) ->
                ChatHistory.add(message.getString()));
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) ChatHistory.add(message.getString());
        });

        // Buttons im Chat-Fenster
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof ChatScreen && AngleConfig.get().chatButtons) {
                ScreenInvoker in = (ScreenInvoker) screen;
                int x = w - 150;
                int y = h - 36;
                in.angle$addRenderableWidget(Button.builder(Component.literal("Translate"),
                        b -> client.setScreen(new AngleScreens.TranslatorScreen(null)))
                        .bounds(x, y, 64, 14).build());
                in.angle$addRenderableWidget(Button.builder(Component.literal("\u2192 EN"),
                        b -> translateInput(client, screen))
                        .bounds(x + 68, y, 40, 14).build());
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> AngleConfig.save());
    }

    /** Übersetzt den Text, den du gerade in das Chat-Eingabefeld getippt hast, ins Englische. */
    private static void translateInput(Minecraft client, Screen screen) {
        for (var child : screen.children()) {
            if (child instanceof EditBox box) {
                String text = box.getValue();
                if (text.isBlank() || text.startsWith("/")) return;
                Translator.translate(text, "de", "en", box::setValue, err -> {
                    if (client.player != null) {
                        client.player.displayClientMessage(Component.literal("[Translation] failed: " + err), false);
                    }
                });
                return;
            }
        }
    }
}
