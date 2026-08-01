package dev.efnilite.ip.foundation.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class Strings {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.builder()
            .extractUrls()
            .hexColors()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .useUnusualXRepeatedCharacterHexFormat() // spigot makes me sad :(
            .build();

    /**
     * Colours a list of strings using {@link MiniMessage} and {@link LegacyComponentSerializer}
     *
     * @param strings The list of strings
     * @return the coloured list of strings
     */
    public static List<String> colour(@NotNull List<String> strings) {
        return strings.stream().map(Strings::colour).collect(Collectors.toList());
    }

    /**
     * Colours an array of strings using {@link MiniMessage} and {@link LegacyComponentSerializer}
     *
     * @param strings The array of strings
     * @return the array of strings, but coloured
     */
    @NotNull
    public static String[] colour(@NotNull String... strings) {
        String[] coloured = new String[strings.length];

        int index = 0;
        for (String string : strings) {
            coloured[index] = colour(string);
            index++;
        }

        return coloured;
    }

    /**
     * Colours a string using {@link MiniMessage} and {@link LegacyComponentSerializer}
     *
     * @param string The string
     * @return the coloured string
     */
    @NotNull
    public static String colour(@NotNull String string) {
        Component component = MINI_MESSAGE.deserialize(string); // sanitize input

        return ChatColor.translateAlternateColorCodes(LegacyComponentSerializer.SECTION_CHAR, LEGACY_COMPONENT_SERIALIZER.serialize(component));
    }

    /**
     * Gets the closest matching string
     *
     * @param source  The source string
     * @param strings Strings which will be compared to this string
     * @return the closest matching string from parameter strings
     */
    public static String getClosestMatching(String source, List<String> strings) {
        int min = Integer.MAX_VALUE;
        String closest = "";

        for (String string : strings) {
            int distance = getLevenshteinDistance(source, string);
            if (distance < min) {
                closest = string;
            }
        }
        return closest;
    }

    /**
     * Gets the levenshtein distance between two strings
     * Source: <a href="https://www.stephenenright.com/java-levenshtein-distance">https://www.stephenenright.com/java-levenshtein-distance</a>
     *
     * @param source The source string
     * @param other  The other string
     * @return the distance required
     */
    public static int getLevenshteinDistance(String source, String other) {
        int sourceLength = source.length();
        int otherLength = other.length();

        int[][] minDistanceMatrix = new int[sourceLength + 1][otherLength + 1];  // init the minimum distance matrix and add one to account for default values
        minDistanceMatrix[0][0] = 0;

        for (int row = 1; row <= sourceLength; row++) { // enter default edit values for the source string (in rows)
            minDistanceMatrix[row][0] = row;
        }

        for (int col = 1; col <= otherLength; col++) { // enter default edit values for the other string (in cols)
            minDistanceMatrix[0][col] = col;
        }

        for (int row = 1; row <= sourceLength; row++) {
            for (int col = 1; col <= otherLength; col++) { // go through every value and get the min value
                minDistanceMatrix[row][col] = getMinLevenshteinCost(source, other, minDistanceMatrix, row, col);
            }
        }

        return minDistanceMatrix[sourceLength][otherLength]; // get the last value
    }

    private static int getMinLevenshteinCost(String source, String other, int[][] minDistanceMatrix, int row, int col) {
        int insertion = minDistanceMatrix[row][col - 1] + 1;
        int deletion = minDistanceMatrix[row - 1][col] + 1;
        int substition = minDistanceMatrix[row - 1][col - 1];

        if (source.charAt(row - 1) != other.charAt(col - 1)) { // if the letters are the same skip adding a cost
            substition += 1;
        }

        return Numbers.min(insertion, deletion, substition);
    }
}