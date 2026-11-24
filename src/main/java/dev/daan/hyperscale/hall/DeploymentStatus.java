package dev.daan.hyperscale.hall;

public enum DeploymentStatus {
    RUNNING("running", 0xFF34D399),
    PAUSED("paused", 0xFF94A3B8),
    WAITING_INPUT("waiting_input", 0xFFFBBF24),
    OUTPUT_FULL("output_full", 0xFFF59E0B),
    NO_POWER("no_power", 0xFFF87171),
    OVERHEATED("overheated", 0xFFEF4444),
    INSUFFICIENT_RESOURCES("insufficient_resources", 0xFFFB7185),
    MISSING_IMAGE("missing_image", 0xFFEF4444);

    private final String id;
    private final int color;

    DeploymentStatus(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String id() {
        return this.id;
    }

    public int color() {
        return this.color;
    }

    public static DeploymentStatus byId(String id) {
        for (DeploymentStatus status : values()) {
            if (status.id.equals(id)) {
                return status;
            }
        }
        return PAUSED;
    }
}
