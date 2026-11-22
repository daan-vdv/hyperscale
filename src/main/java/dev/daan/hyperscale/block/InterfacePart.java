package dev.daan.hyperscale.block;

import net.minecraft.util.StringRepresentable;

/** Horizontal half of the 2-wide workbench: primary = placed cell, secondary = model +X. */
public enum InterfacePart implements StringRepresentable {
    PRIMARY("primary"),
    SECONDARY("secondary");

    private final String name;

    InterfacePart(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
