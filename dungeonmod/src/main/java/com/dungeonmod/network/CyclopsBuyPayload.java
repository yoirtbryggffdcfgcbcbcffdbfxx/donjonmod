package com.dungeonmod.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CyclopsBuyPayload(int syncId, int tradeIndex, int quantity) implements CustomPayload {
    public static final CustomPayload.Id<CyclopsBuyPayload> ID = new CustomPayload.Id<>(Identifier.of("dungeonmod", "cyclops_buy"));
    public static final PacketCodec<PacketByteBuf, CyclopsBuyPayload> CODEC = PacketCodec.of(
        (value, buf) -> { buf.writeVarInt(value.syncId); buf.writeVarInt(value.tradeIndex); buf.writeVarInt(value.quantity); },
        buf -> new CyclopsBuyPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt())
    );
    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
