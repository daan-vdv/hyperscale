package dev.daan.hyperscale.block;

import net.minecraft.util.StringRepresentable;

public enum CableStraight implements StringRepresentable {
    NONE("none"),
    X("x"),
    Y("y"),
    Z("z");

    private final String serializedName;

    CableStraight(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
