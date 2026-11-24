package dev.daan.hyperscale.network;

import dev.daan.hyperscale.Hyperscale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record PortFilterPayload(BlockPos portPos, int slot, ItemStack filterItem) implements CustomPacketPayload {
    public static final Type<PortFilterPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Hyperscale.MOD_ID, "port_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PortFilterPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            PortFilterPayload::portPos,
            ByteBufCodecs.VAR_INT,
            PortFilterPayload::slot,
            ItemStack.OPTIONAL_STREAM_CODEC,
            PortFilterPayload::filterItem,
            PortFilterPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
