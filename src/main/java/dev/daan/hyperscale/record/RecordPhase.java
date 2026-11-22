package dev.daan.hyperscale.record;

public enum RecordPhase {
    IDLE,
    LEARNING,
    DONE,
    FAILED;

    public static RecordPhase byId(int id) {
        RecordPhase[] values = values();
        if (id < 0 || id >= values.length) {
            return IDLE;
        }
        return values[id];
    }
}
