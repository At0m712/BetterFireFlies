package com.atom.firefly.fabric.network;

import com.atom.firefly.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CatchFireflyPayload(boolean mainHand) implements CustomPacketPayload {
    public static final Type<CatchFireflyPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "catch_firefly")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CatchFireflyPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            CatchFireflyPayload::mainHand,
            CatchFireflyPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
