package dev.daan.hyperscale.hall;

public enum RaidMode {
    JBOD("jbod", "JBOD", 1),
    RAID1("raid1", "RAID 1", 2),
    RAID5("raid5", "RAID 5", 3);

    private final String id;
    private final String label;
    private final int minDrives;

    RaidMode(String id, String label, int minDrives) {
        this.id = id;
        this.label = label;
        this.minDrives = minDrives;
    }

    public String id() {
        return this.id;
    }

    public String label() {
        return this.label;
    }

    public int minDrives() {
        return this.minDrives;
    }

    public RaidMode next() {
        RaidMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static RaidMode byId(String id) {
        for (RaidMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return JBOD;
    }
}
