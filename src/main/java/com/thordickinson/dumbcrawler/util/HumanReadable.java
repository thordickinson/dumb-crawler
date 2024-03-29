package com.thordickinson.dumbcrawler.util;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.Duration;

public class HumanReadable {
    private HumanReadable() {

    }

    public static String formatBits(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }

    public static String formatDuration(Duration duration) {
        return String.format("%sd %sh %sm %ss",
                duration.toDaysPart(),
                duration.toHoursPart(),
                duration.toMinutesPart(),
                duration.toSecondsPart());
    }

    public static String formatDuration(long start, long end) {
        return formatDuration(Duration.ofMillis(end - start));
    }

    public static String formatDuration(long start) {
        return formatDuration(start, System.currentTimeMillis());
    }

}
