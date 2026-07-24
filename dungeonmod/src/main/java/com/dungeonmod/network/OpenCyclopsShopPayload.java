package com.dungeonmod.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenCyclopsShopPayload() implements CustomPayload {
    public static final CustomPayload.Id<OpenCyclopsShopPayload> ID = new CustomPayload.Id<>(Identifier.of("dungeonmod", "open_cyclops_shop"));

    public static final PacketCodec<PacketByteBuf, OpenCyclopsShopPayload> CODEC = PacketCodec.of(
        (value, buf) -> {},
        buf -> new OpenCyclopsShopPayload()
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
