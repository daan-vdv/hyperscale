package dev.daan.hyperscale.network;

import dev.daan.hyperscale.Hyperscale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record InterfaceActionPayload(BlockPos interfacePos, int actionId, String imageKey, int amount) implements CustomPacketPayload {
    public static final int ACTION_DEPLOY = 0;
    public static final int ACTION_SCALE_UP = 1;
    public static final int ACTION_SCALE_DOWN = 2;
    public static final int ACTION_TOGGLE_PAUSE = 3;
    public static final int ACTION_REMOVE = 4;
    public static final int ACTION_EXTRACT_STORAGE = 5;
    public static final int ACTION_CYCLE_PROFILE = 6;

    public static final Type<InterfaceActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Hyperscale.MOD_ID, "interface_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InterfaceActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            InterfaceActionPayload::interfacePos,
            ByteBufCodecs.VAR_INT,
            InterfaceActionPayload::actionId,
            ByteBufCodecs.STRING_UTF8,
            InterfaceActionPayload::imageKey,
            ByteBufCodecs.VAR_INT,
            InterfaceActionPayload::amount,
            InterfaceActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
