package dev.daan.hyperscale.hall;

public enum ExecutionProfile {
    ECO("eco", "Eco", 0.75D, 0.60D, 0.55D),
    BALANCED("balanced", "Bal", 1.00D, 1.00D, 1.00D),
    OVERCLOCK("overclock", "OC", 1.50D, 2.00D, 2.20D);

    private final String id;
    private final String shortLabel;
    private final double speedMultiplier;
    private final double energyMultiplier;
    private final double heatMultiplier;

    ExecutionProfile(String id, String shortLabel, double speedMultiplier, double energyMultiplier, double heatMultiplier) {
        this.id = id;
        this.shortLabel = shortLabel;
        this.speedMultiplier = speedMultiplier;
        this.energyMultiplier = energyMultiplier;
        this.heatMultiplier = heatMultiplier;
    }

    public String id() {
        return this.id;
    }

    public String shortLabel() {
        return this.shortLabel;
    }

    public double speedMultiplier() {
        return this.speedMultiplier;
    }

    public double energyMultiplier() {
        return this.energyMultiplier;
    }

    public double heatMultiplier() {
        return this.heatMultiplier;
    }

    public ExecutionProfile next() {
        ExecutionProfile[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static ExecutionProfile byId(String id) {
        for (ExecutionProfile profile : values()) {
            if (profile.id.equals(id)) {
                return profile;
            }
        }
        return BALANCED;
    }
}
