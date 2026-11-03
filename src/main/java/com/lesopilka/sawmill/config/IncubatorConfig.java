package com.lesopilka.sawmill.config;

public final class IncubatorConfig {
    private IncubatorConfig() {}

    public static final int BASE_PROCESS_TICKS = SawmillConfig.BASE_PROCESS_TICKS;

    public static final int MIN_PROCESS_TICKS = SawmillConfig.MIN_PROCESS_TICKS;

    public static final int RATE_LIMIT_INTERVAL = SawmillConfig.RATE_LIMIT_INTERVAL;

    public static final int LOGS_MIN = 3;

    public static final int LOGS_MAX = 5;

    public static final int LOGS_HARD_CAP = 9;

    public static final int SAPLINGS_MAX = 2;

    public static final int BONE_MEAL_MIN = 1;
    public static final int BONE_MEAL_MAX = 5;

    public static final int FUEL_COAL = SawmillConfig.FUEL_COAL;
    public static final int FUEL_PLANKS = SawmillConfig.FUEL_PLANKS;
    public static final int FUEL_LAVA_BUCKET = SawmillConfig.FUEL_LAVA_BUCKET;

    public static final int AUTO_PUSH_INTERVAL = SawmillConfig.AUTO_PUSH_INTERVAL;

    public static final int AUTO_UNJAM_INTERVAL = SawmillConfig.AUTO_UNJAM_INTERVAL;

    public static final int MAINTENANCE_GRACE_TICKS = SawmillConfig.MAINTENANCE_GRACE_TICKS;
}
