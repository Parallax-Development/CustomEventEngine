package com.darkbladedev.cee.util;

public final class DurationParser {
    private DurationParser() {
    }

    public static long parseTicks(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim().toLowerCase();
        if (text.isEmpty()) {
            return 0L;
        }
        long multiplier = 1L;
        if (text.endsWith("ms")) {
            long ms = Long.parseLong(text.substring(0, text.length() - 2));
            return Math.max(1L, ms / 50L);
        }
        if (text.endsWith("s")) {
            multiplier = 20L;
            text = text.substring(0, text.length() - 1);
        } else if (text.endsWith("m")) {
            multiplier = 20L * 60L;
            text = text.substring(0, text.length() - 1);
        } else if (text.endsWith("h")) {
            multiplier = 20L * 60L * 60L;
            text = text.substring(0, text.length() - 1);
        } else if (text.endsWith("t")) {
            text = text.substring(0, text.length() - 1);
        }
        return Long.parseLong(text) * multiplier;
    }
}
