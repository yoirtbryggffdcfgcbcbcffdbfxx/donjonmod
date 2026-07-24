package com.dungeonmod.network;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CyclopsSellPayload(int syncId, int tradeIndex, ItemStack rewardItem) implements CustomPayload {
    public static final CustomPayload.Id<CyclopsSellPayload> ID = new CustomPayload.Id<>(Identifier.of("dungeonmod", "cyclops_sell"));
    public static final PacketCodec<PacketByteBuf, CyclopsSellPayload> CODEC = PacketCodec.of(
        (value, buf) -> { buf.writeVarInt(value.syncId); buf.writeVarInt(value.tradeIndex); ItemStack.OPTIONAL_PACKET_CODEC.encode((RegistryByteBuf) buf, value.rewardItem); },
        buf -> new CyclopsSellPayload(buf.readVarInt(), buf.readVarInt(), ItemStack.OPTIONAL_PACKET_CODEC.decode((RegistryByteBuf) buf))
    );
    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
