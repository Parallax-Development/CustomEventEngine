package com.darkbladedev.cee.util;

import com.darkbladedev.cee.api.EventContext;

import org.mvel2.MVEL;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ValueResolver {
    private static final Pattern BRACE_PATTERN = Pattern.compile("\\{([A-Za-z_][A-Za-z0-9_]*)\\}");
    private static final Pattern DOLLAR_PATTERN = Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)\\}");
    private static final Map<String, Serializable> EXPRESSION_CACHE = new ConcurrentHashMap<>();

    private ValueResolver() {
    }

    public static Object resolveValue(Object raw, EventContext context) {
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof String str)) {
            return raw;
        }

        String trimmed = str.trim();
        if (trimmed.startsWith("=")) {
            String expr = trimmed.substring(1).trim();
            Serializable compiled = EXPRESSION_CACHE.computeIfAbsent(expr, MVEL::compileExpression);
            return MVEL.executeExpression(compiled, context.getVariables());
        }

        if (trimmed.startsWith("${") && trimmed.endsWith("}") && trimmed.length() > 3) {
            String ref = trimmed.substring(2, trimmed.length() - 1).trim();
            return context.getVariable(ref);
        }

        return resolveText(str, context);
    }

    public static String resolveText(String template, EventContext context) {
        if (template == null || template.isEmpty()) {
            return "";
        }

        String output = replacePattern(template, DOLLAR_PATTERN, context);
        output = replacePattern(output, BRACE_PATTERN, context);
        return output;
    }

    private static String replacePattern(String input, Pattern pattern, EventContext context) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = context.getVariable(key);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
