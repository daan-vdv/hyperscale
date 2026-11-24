package dev.daan.hyperscale.registry;

import dev.daan.hyperscale.Hyperscale;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final TagKey<Block> COMPILER_FORBIDDEN = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Hyperscale.MOD_ID, "compiler_forbidden")
    );
    public static final TagKey<Block> COMPILER_ALLOWED = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Hyperscale.MOD_ID, "compiler_allowed")
    );

    private ModTags() {
    }
}
