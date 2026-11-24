package dev.daan.hyperscale;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class HyperscaleConfig {
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.BooleanValue USE_ALLOWLIST;
    public static final ModConfigSpec.IntValue ENERGY_DRIFT_TOLERANCE;
    public static final ModConfigSpec.DoubleValue HALL_ENERGY_MULTIPLIER;
    public static final ModConfigSpec.IntValue BASE_REPLICA_FE_PER_TICK;
    public static final ModConfigSpec.BooleanValue ENABLE_THERMAL;
    public static final ModConfigSpec.DoubleValue WEAR_RATE_MULTIPLIER;
    public static final ModConfigSpec.IntValue WATERCOOLER_MB_PER_TICK;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("compiler");
        USE_ALLOWLIST = builder
                .comment("If true, ONLY blocks in #hyperscale:compiler_allowed (plus Hyperscale probes) may be placed inside a Compiler Casing room.")
                .define("useAllowlist", false);
        ENERGY_DRIFT_TOLERANCE = builder
                .comment("Allowed FE difference between start and end baseline inside the sealed casing to tolerate minor modded energy-buffer jitter.")
                .defineInRange("energyDriftTolerance", 100, 0, 100_000);
        builder.pop();

        builder.push("hall");
        HALL_ENERGY_MULTIPLIER = builder
                .comment("Multiplier applied to recorded FE per cycle when running Images in a datacenter hall (e.g. 1.0 = exact recorded cost, 1.5 = +50% overhead).")
                .defineInRange("energyMultiplier", 1.0D, 0.0D, 100.0D);
        BASE_REPLICA_FE_PER_TICK = builder
                .comment("Base compute FE/t overhead per active replica (even for 0-FE vanilla images).")
                .defineInRange("baseReplicaFePerTick", 20, 0, 10_000);
        ENABLE_THERMAL = builder
                .comment("Enable datacenter heat, cooling (Server Fans & Watercoolers), and non-destructive thermal throttling.")
                .define("enableThermal", true);
        WEAR_RATE_MULTIPLIER = builder
                .comment("Multiplier for hardware wear speed (0.0 disables hardware wear, 1.0 is default).")
                .defineInRange("wearRateMultiplier", 1.0D, 0.0D, 100.0D);
        WATERCOOLER_MB_PER_TICK = builder
                .comment("Base water consumption (mB/tick) per active Watercooler Controller when the hall is warm (reduced up to 75% by adjacent Server Fan radiators; 0 disables consumption).")
                .defineInRange("watercoolerMbPerTick", 20, 0, 1_000);
        builder.pop();

        SERVER_SPEC = builder.build();
    }

    private HyperscaleConfig() {
    }
}
