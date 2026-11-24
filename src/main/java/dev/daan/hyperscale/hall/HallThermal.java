package dev.daan.hyperscale.hall;

import dev.daan.hyperscale.HyperscaleConfig;
import dev.daan.hyperscale.blockentity.WatercoolerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Thermal, cooling, and acoustic noise model for a datacenter hall.
 */
public record HallThermal(
        int fanCount,
        int watercoolerCount,
        int liquidWatercoolers,
        int coolingWatts,
        int coolingFePerTick,
        int noiseDb
) {
    public static final double AMBIENT_TEMP_C = 22.0D;
    public static final double COLD_TURBO_TEMP_C = 50.0D;
    public static final double OVERCLOCK_MAX_TEMP_C = 55.0D;
    public static final double THROTTLE_START_TEMP_C = 80.0D;
    public static final double SHUTDOWN_TEMP_C = 100.0D;
    public static final double RECOVERY_TEMP_C = 78.0D;

    public static final int PASSIVE_COOLING_W = 500;
    public static final int FAN_COOLING_W = 600;
    public static final int FAN_FE_PER_TICK = 15;
    public static final int WATERCOOLER_DRY_W = 1_200;
    public static final int WATERCOOLER_LIQUID_W = 4_000;
    public static final int WATERCOOLER_FE_PER_TICK = 120;

    public static HallThermal compute(Level level, HallParts parts) {
        return computeAndConsume(level, parts, AMBIENT_TEMP_C, false);
    }

    /**
     * Computes hall cooling capacity and, when {@code consumeCoolant} is true and the hall is warm,
     * actively draws water from each Watercooler Controller's internal tank (reduced up to 75% by
     * adjacent Server Fan radiators).
     */
    public static HallThermal computeAndConsume(Level level, HallParts parts, double currentTempC, boolean consumeCoolant) {
        int fans = parts.fans.size();
        int coolers = parts.watercoolers.size();
        int liquidCoolers = 0;

        boolean warm = currentTempC > AMBIENT_TEMP_C + 2.0D;
        int baseMb = HyperscaleConfig.WATERCOOLER_MB_PER_TICK.get();
        // Scale coolant usage smoothly from 0.5x at 24°C up to 2.0x at 80°C+
        double loadScale = warm
                ? Math.max(0.5D, Math.min(2.0D, 0.5D + ((currentTempC - AMBIENT_TEMP_C) / 58.0D) * 1.5D))
                : 0.0D;

        for (BlockPos pos : parts.watercoolers) {
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof WatercoolerBlockEntity cooler)) {
                continue;
            }
            if (consumeCoolant && warm && baseMb > 0) {
                double radiatorFactor = WatercoolerBlockEntity.radiatorConsumptionFactor(level, pos);
                int needMb = Math.max(1, (int) Math.round(baseMb * loadScale * radiatorFactor));
                if (cooler.consumeCoolant(needMb)) {
                    liquidCoolers++;
                }
            } else if (cooler.hasCoolant()) {
                liquidCoolers++;
            }
        }

        int dryCoolers = coolers - liquidCoolers;
        int coolingW = PASSIVE_COOLING_W
                + fans * FAN_COOLING_W
                + dryCoolers * WATERCOOLER_DRY_W
                + liquidCoolers * WATERCOOLER_LIQUID_W;
        int coolingFe = fans * FAN_FE_PER_TICK + coolers * WATERCOOLER_FE_PER_TICK;

        int noise = 24;
        if (fans > 0) {
            noise += (int) Math.round(18.0D + 12.0D * Math.log(fans));
        }
        if (coolers > 0) {
            noise += Math.min(8, coolers * 2);
        }

        return new HallThermal(fans, coolers, liquidCoolers, coolingW, coolingFe, noise);
    }

    public static boolean hasWaterSupply(Level level, BlockPos pos) {
        return level.isLoaded(pos)
                && level.getBlockEntity(pos) instanceof WatercoolerBlockEntity cooler
                && cooler.hasCoolant();
    }

    public boolean hasLiquidCooling() {
        return this.liquidWatercoolers > 0;
    }

    /**
     * Effective thermal speed multiplier based on current hall temperature and liquid cooling.
     */
    public static double speedFactor(double tempC, boolean hasLiquidCooling, boolean shutdown) {
        if (shutdown) {
            return 0.0D;
        }
        if (hasLiquidCooling && tempC <= COLD_TURBO_TEMP_C) {
            return 1.15D;
        }
        if (tempC <= THROTTLE_START_TEMP_C) {
            return 1.00D;
        }
        double over = Math.min(1.0D, (tempC - THROTTLE_START_TEMP_C) / (SHUTDOWN_TEMP_C - THROTTLE_START_TEMP_C));
        return Math.max(0.40D, 1.00D - 0.60D * over);
    }

    public static double nextTemperature(double currentTempC, int heatWatts, int coolingWatts) {
        double ratio = (double) Math.max(0, heatWatts) / Math.max(200.0D, coolingWatts);
        double target = Math.min(115.0D, AMBIENT_TEMP_C + ratio * 58.0D);
        return currentTempC + (target - currentTempC) * 0.05D;
    }
}
