package dev.daan.hyperscale.block;

import net.minecraft.util.StringRepresentable;

public enum CableConnection implements StringRepresentable {
    NONE("none"),
    CABLE("cable"),
    MACHINE("machine");

    private final String serializedName;

    CableConnection(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
