package pl.tpahere.auto;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TpaHereClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("tpahere");

    private static KeyBinding openKey;
    private static KeyBinding toggleKey;

    private static final Deque<String> QUEUE = new ArrayDeque<>();
    private static final Set<String> QUEUED = new HashSet<>();
    private static final Map<String, Long> LAST_SENT = new HashMap<>();
    private static int wait = 0;

    @Override
    public void onInitializeClient() {
        Config.load();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("tpahere", "main"));
        openKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.tpahere.open_gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, category));
        toggleKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.tpahere.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, category));

        // Wiadomosci czatu podpisane (zwykly czat serwera) - znamy nadawce
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, ts) -> {
            if (sender != null) {
                onChatter(sender.name());
            }
        });

        // Wiadomosci "systemowe" - wiele serwerow wysyla tak czat (pluginy). Szukamy nicku przed ':' lub '>'
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                handleGameMessage(message.getString());
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                client.setScreen(new TpaHereScreen());
            }
            while (toggleKey.wasPressed()) {
                Config.enabled = !Config.enabled;
                Config.save();
                if (client.player != null) {
                    client.player.sendMessage(Text.translatable(Config.enabled ? "tpahere.chat_on" : "tpahere.chat_off")
                            .formatted(Config.enabled ? Formatting.GREEN : Formatting.RED), true);
                }
            }
            tick(client);
        });
    }

    private static void handleGameMessage(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Config.enabled || mc.getNetworkHandler() == null) return;

        int cut = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ':' || c == '>' || c == '\u00BB') {
                cut = i;
                break;
            }
        }
        if (cut <= 0) return;
        String prefix = text.substring(0, cut);

        for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
            String name = e.getProfile().name();
            if (name == null || name.isEmpty()) continue;
            if (containsWord(prefix, name)) {
                onChatter(name);
                return;
            }
        }
    }

    private static boolean containsWord(String text, String word) {
        int from = 0;
        while (true) {
            int idx = text.indexOf(word, from);
            if (idx < 0) return false;
            boolean leftOk = idx == 0 || !isNickChar(text.charAt(idx - 1));
            int end = idx + word.length();
            boolean rightOk = end >= text.length() || !isNickChar(text.charAt(end));
            if (leftOk && rightOk) return true;
            from = idx + 1;
        }
    }

    private static boolean isNickChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static void onChatter(String name) {
        if (!Config.enabled || name == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        String self = mc.getSession().getUsername();
        if (self != null && self.equalsIgnoreCase(name)) return;

        String key = name.toLowerCase(Locale.ROOT);
        if (QUEUED.contains(key)) return;
        Long last = LAST_SENT.get(key);
        long cooldownMs = Config.cooldownMinutes * 60_000L;
        if (last != null && System.currentTimeMillis() - last < cooldownMs) return;

        QUEUE.add(name);
        QUEUED.add(key);
    }

    private static void tick(MinecraftClient client) {
        if (!Config.enabled) {
            if (!QUEUE.isEmpty()) {
                QUEUE.clear();
                QUEUED.clear();
            }
            return;
        }
        if (client.player == null || client.getNetworkHandler() == null) return;
        if (wait > 0) {
            wait--;
            return;
        }
        String name = QUEUE.poll();
        if (name == null) return;
        QUEUED.remove(name.toLowerCase(Locale.ROOT));
        LAST_SENT.put(name.toLowerCase(Locale.ROOT), System.currentTimeMillis());
        client.getNetworkHandler().sendChatCommand("tpahere " + name);
        wait = Config.delaySeconds * 20;
    }
}
