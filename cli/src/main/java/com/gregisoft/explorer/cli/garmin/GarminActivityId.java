package com.gregisoft.explorer.cli.garmin;

import java.math.BigInteger;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record GarminActivityId(String value) {

    private static final Pattern DECIMAL = Pattern.compile("[0-9]+");
    private static final Pattern ACTIVITY_PATH = Pattern.compile("/modern/activity/([0-9]+)/?");

    public GarminActivityId {
        if (value == null || !value.matches("[1-9][0-9]*")) {
            throw new IllegalArgumentException("Garmin activity ID must be a positive integer.");
        }
    }

    public static GarminActivityId parse(String input) {
        if (input == null || input.isBlank()) {
            throw invalid();
        }
        String candidate = input.trim();
        if (DECIMAL.matcher(candidate).matches()) {
            return canonical(candidate);
        }

        URI uri;
        try {
            uri = URI.create(candidate);
        } catch (IllegalArgumentException exception) {
            throw invalid();
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"connect.garmin.com".equalsIgnoreCase(uri.getHost())
                || uri.getUserInfo() != null
                || uri.getPort() != -1
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw invalid();
        }
        Matcher path = ACTIVITY_PATH.matcher(uri.getPath());
        if (!path.matches()) {
            throw invalid();
        }
        return canonical(path.group(1));
    }

    private static GarminActivityId canonical(String decimal) {
        BigInteger parsed = new BigInteger(decimal);
        if (parsed.signum() <= 0) {
            throw invalid();
        }
        return new GarminActivityId(parsed.toString());
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException(
                "Expected a positive Garmin activity ID or an HTTPS Garmin Connect activity URL."
        );
    }
}
