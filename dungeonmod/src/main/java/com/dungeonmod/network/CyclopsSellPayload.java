package com.dungeonmod.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CyclopsSellPayload(int syncId, int tradeIndex) implements CustomPayload {
    public static final CustomPayload.Id<CyclopsSellPayload> ID = new CustomPayload.Id<>(Identifier.of("dungeonmod", "cyclops_sell"));
    public static final PacketCodec<PacketByteBuf, CyclopsSellPayload> CODEC = PacketCodec.of(
        (value, buf) -> { buf.writeVarInt(value.syncId); buf.writeVarInt(value.tradeIndex); },
        buf -> new CyclopsSellPayload(buf.readVarInt(), buf.readVarInt())
    );
    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
