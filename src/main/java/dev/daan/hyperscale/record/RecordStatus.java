package dev.daan.hyperscale.record;

public enum RecordStatus {
    IDLE("gui.hyperscale.recorder.status.idle"),
    NEED_CARD("gui.hyperscale.recorder.status.need_card"),
    NEED_EMPTY("gui.hyperscale.recorder.status.need_empty"),
    NEED_BIND("gui.hyperscale.recorder.status.need_bind"),
    NEED_ITEMS("gui.hyperscale.recorder.status.need_items"),
    SAMPLING("gui.hyperscale.recorder.status.sampling"),
    RUN_DONE("gui.hyperscale.recorder.status.run_done"),
    DONE("gui.hyperscale.recorder.status.done"),
    TOO_LARGE("gui.hyperscale.recorder.status.too_large"),
    EMPTY("gui.hyperscale.recorder.status.empty"),
    DRIFT("gui.hyperscale.recorder.status.drift"),
    MISMATCH("gui.hyperscale.recorder.status.mismatch"),
    PASSTHROUGH("gui.hyperscale.recorder.status.passthrough"),
    LOST_BIND("gui.hyperscale.recorder.status.lost_bind"),
    INTERRUPTED("gui.hyperscale.recorder.status.interrupted"),
    NO_CASING("gui.hyperscale.recorder.status.no_casing"),
    SEAL_BROKEN("gui.hyperscale.recorder.status.seal_broken"),
    PLAYER_INSIDE("gui.hyperscale.recorder.status.player_inside"),
    ENTITY_INSIDE("gui.hyperscale.recorder.status.entity_inside"),
    FORBIDDEN_BLOCK("gui.hyperscale.recorder.status.forbidden_block"),
    ENERGY_DRIFT("gui.hyperscale.recorder.status.energy_drift"),
    FLUID_DRIFT("gui.hyperscale.recorder.status.fluid_drift"),
    ENERGY_MISMATCH("gui.hyperscale.recorder.status.energy_mismatch"),
    BLOCK_DRIFT("gui.hyperscale.recorder.status.block_drift");

    private final String key;

    RecordStatus(String key) {
        this.key = key;
    }

    public String key() {
        return this.key;
    }

    public static RecordStatus byId(int id) {
        RecordStatus[] values = values();
        if (id < 0 || id >= values.length) {
            return IDLE;
        }
        return values[id];
    }
}
