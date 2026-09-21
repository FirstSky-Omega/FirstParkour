package fr.firstsky.firstparkour.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageUtil {

    private MessageUtil() {}

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /** Convertit &x et &#RRGGBB en codes §. Utilisé pour les titres d'inventaire (legacy string). */
    public static String color(String s) {
        s = translateHex(s);
        // Translate &a, &b, &l, etc.
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(chars[i + 1]) > -1) {
                chars[i] = '§';
            }
        }
        return new String(chars);
    }

    /** Convertit &#RRGGBB → §x§R§R§G§G§B§B (format legacy Minecraft). */
    private static String translateHex(String s) {
        Matcher m = HEX_PATTERN.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String hex = m.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static Component component(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(component(message));
    }

    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(component(message));
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.showTitle(net.kyori.adventure.title.Title.title(
                component(title),
                component(subtitle),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(fadeIn * 50L),
                        java.time.Duration.ofMillis(stay * 50L),
                        java.time.Duration.ofMillis(fadeOut * 50L))));
    }
}
