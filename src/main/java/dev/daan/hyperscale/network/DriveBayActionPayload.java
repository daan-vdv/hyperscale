package dev.daan.hyperscale.network;

import dev.daan.hyperscale.Hyperscale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DriveBayActionPayload(BlockPos bayPos, int actionId) implements CustomPacketPayload {
    public static final int ACTION_CYCLE_RAID = 0;
    public static final int ACTION_SALVAGE_FAILED = 1;

    public static final Type<DriveBayActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Hyperscale.MOD_ID, "drive_bay_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DriveBayActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            DriveBayActionPayload::bayPos,
            ByteBufCodecs.VAR_INT,
            DriveBayActionPayload::actionId,
            DriveBayActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
