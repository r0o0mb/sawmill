package com.lesopilka.sawmill.config;

public final class SawmillConfig {
    private SawmillConfig() {}

    public static final int BASE_PROCESS_TICKS = 120;

    public static final int MIN_PROCESS_TICKS = 40;

    public static final int MAX_LOGS_PER_MINUTE = 10;

    public static final int RATE_LIMIT_INTERVAL = 1200 / MAX_LOGS_PER_MINUTE;

    public static final double JAM_CHANCE = 0.05;

    public static final int MAINTENANCE_GRACE_TICKS = 600;

    public static final int AUTO_UNJAM_INTERVAL = 200;

    public static final int WORK_SOUND_INTERVAL = 30;

    public static final int AUTO_PUSH_INTERVAL = 40;

    public static final int PLANKS_PER_LOG = 4;

    public static final double BONUS_CHANCE_PER_LEVEL = 0.25;

    public static final double SPEED_LEVEL_MULTIPLIER = 0.75;

    public static final int FUEL_COAL = 1600;
    public static final int FUEL_CHARCOAL = 1600;
    public static final int FUEL_PLANKS = 300;
    public static final int FUEL_LAVA_BUCKET = 20000;

    public static final int WRENCH_REPAIR_COST = 1;

    public static final int SELF_DIAG_IRON_COST = 1;
}
