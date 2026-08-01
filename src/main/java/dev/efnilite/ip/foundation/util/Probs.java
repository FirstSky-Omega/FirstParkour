package dev.efnilite.ip.foundation.util;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class Probs {

    /**
     * @return A random chance with range 0 (inclusive) to 1 (exclusive).
     */
    public static double random() {
        return random(ThreadLocalRandom.current());
    }

    /**
     * @param random The random instance.
     * @return A random chance with range 0 (inclusive) to 1 (exclusive).
     */
    public static double random(Random random) {
        return random.nextDouble();
    }

    /**
     * @param mean The average value.
     * @param sd   The standard deviation.
     * @param x    The x-coordinate.
     * @return The normal distributed chance at x.
     */
    public static double normalpdf(double mean, double sd, double x) {
        double a = (x - mean) / sd;

        return (1 / (sd * Math.sqrt(2 * Math.PI))) * Math.exp(-0.5 * (a * a));
    }

    /**
     * @param distribution A map where each key is mapped to a probability.
     * @param random       The random instance.
     * @param <K>          The key type.
     * @return A random item from the list, based on the probabilities.
     */
    public static <K> K random(@NotNull Map<K, Double> distribution, Random random) {
        if (distribution.isEmpty()) {
            throw new NoSuchElementException("No elements in distribution map");
        }

        double total = 0;

        Map<K, Double> cumulative = new LinkedHashMap<>();
        for (Map.Entry<K, Double> entry : distribution.entrySet()) {
            total += entry.getValue();
            cumulative.put(entry.getKey(), total);
        }

        double randomNum = random(random) * total;
        for (Map.Entry<K, Double> entry : cumulative.entrySet()) {
            if (entry.getValue() >= randomNum) {
                return entry.getKey();
            }
        }

        return distribution.keySet().stream().findFirst().orElseThrow();
    }

    /**
     * @param distribution A map where each key is mapped to a probability.
     * @param <K>          The key type.
     * @return A random item from the list, based on the probabilities.
     */
    public static <K> K random(@NotNull Map<K, Double> distribution) {
        return random(distribution, ThreadLocalRandom.current());
    }
}