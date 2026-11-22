package dev.daan.hyperscale.block;

import net.minecraft.util.StringRepresentable;

public enum PortMode implements StringRepresentable {
    INPUT("input"),
    OUTPUT("output");

    private final String serializedName;

    PortMode(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
