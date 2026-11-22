package dev.daan.hyperscale.hall;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public record HallImage(BlockPos drive, int slot, ProcessImage image, ItemStack stack) {
}
