package dev.efnilite.ip.foundation.util;

import java.util.HashMap;

/**
 * Time utils
 *
 * @author Efnilite
 */
public class Time {

    private static final HashMap<String, Long> timings = new HashMap<>();

    /**
     * Hours per day.
     */
    public static final int HOURS_PER_DAY = 24;

    /**
     * Minutes per hour.
     */
    public static final int MINUTES_PER_HOUR = 60;

    /**
     * Seconds per minute.
     */
    public static final int SECONDS_PER_MINUTE = 60;

    /**
     * Seconds per hour.
     */
    public static final int SECONDS_PER_HOUR = 3600;

    /**
     * Seconds per day.
     */
    public static final int SECONDS_PER_DAY = 86400;

    /**
     * Converts an amount of seconds to millis
     *
     * @param secs The second count
     * @return the second count to millis
     */
    public static long toMillis(long secs) {
        return secs * 1000;
    }

    /**
     * Start the timer.
     *
     * @param key The name of the operation which will be timed. These need to be unique!
     */
    public static void timerStart(String key) {
        timings.put(key, System.currentTimeMillis());
    }


    /**
     * End the timer and get the time between start and finish in ms.
     *
     * @param key The name of the operation.
     * @return the time it took between starting and finishing.
     */
    public static long timerEnd(String key) {
        long startTime = timings.get(key);
        timings.remove(key);
        return System.currentTimeMillis() - startTime;
    }

}