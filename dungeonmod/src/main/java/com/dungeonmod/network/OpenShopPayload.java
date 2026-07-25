package com.dungeonmod.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenShopPayload() implements CustomPayload {
    public static final CustomPayload.Id<OpenShopPayload> ID = new CustomPayload.Id<>(Identifier.of("dungeonmod", "open_shop"));

    public static final PacketCodec<PacketByteBuf, OpenShopPayload> CODEC = PacketCodec.of(
        (value, buf) -> {},
        buf -> new OpenShopPayload()
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
