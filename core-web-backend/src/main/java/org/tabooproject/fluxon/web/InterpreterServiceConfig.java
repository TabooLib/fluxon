package org.tabooproject.fluxon.web;

class InterpreterServiceConfig {
    private final long costLimit;
    private final long costPerStep;

    InterpreterServiceConfig(long costLimit, long costPerStep) {
        this.costLimit = costLimit;
        this.costPerStep = costPerStep;
    }

    static InterpreterServiceConfig fromSystem() {
        long limit = parseLong(System.getProperty("fluxon.costLimit"), 10_000L);
        long step = parseLong(System.getProperty("fluxon.costPerStep"), 1L);
        return new InterpreterServiceConfig(limit, step);
    }

    private static long parseLong(String value, long def) {
        if (value == null || value.trim().isEmpty()) {
            return def;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return def;
        }
    }

    long getCostLimit() {
        return costLimit;
    }

    long getCostPerStep() {
        return costPerStep;
    }
}
