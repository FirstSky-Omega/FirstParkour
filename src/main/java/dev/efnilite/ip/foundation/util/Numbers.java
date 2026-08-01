package dev.efnilite.ip.foundation.util;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Number utils
 */
public class Numbers {

    /**
     * Returns a random real number between two bounds.
     *
     * @param lower The lower bound (inclusive)
     * @param upper The upper bound (exclusive)
     * @return a random real number between the two bounds
     */
    public static int random(int lower, int upper) {
        return ThreadLocalRandom.current().nextInt(lower, upper);
    }

    /**
     * Gets the min int from an Array of ints
     *
     * @param ints All ints
     * @return the smallest of the values of parameter ints
     */
    public static int min(int... ints) {
        return Arrays.stream(ints).min().orElseThrow();
    }

    /**
     * Gets the max int from an array of ints
     *
     * @param ints All ints
     * @return the int with the biggest value
     */
    public static int max(int... ints) {
        return Arrays.stream(ints).max().orElseThrow();
    }

    /**
     * Returns all real numbers between two bounds
     *
     * @param from The lower bound (inclusive)
     * @param to   The upper bound (inclusive)
     * @return a list of all real numbers between the two bounds
     */
    public static List<Integer> getFromTo(int from, int to) {
        return IntStream.rangeClosed(min(from, to), max(from, to)).boxed().collect(Collectors.toList());
    }

    /**
     * Gets all ints from 0 (inclusive) to n (inclusive)
     *
     * @param n The max value
     * @return a list with all ints from 0 to n
     */
    public static List<Integer> getFromZero(int n) {
        return getFromTo(0, n);
    }
}
