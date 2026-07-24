package com.dungeonmod.network;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

public record CyclopsTradesPayload(int syncId, String npcId, String npcName, boolean hasBuyMode, boolean hasSellMode, List<TradeData> trades) implements CustomPayload {
    public static final CustomPayload.Id<CyclopsTradesPayload> ID = new CustomPayload.Id<>(Identifier.of("dungeonmod", "cyclops_trades"));
    public static final PacketCodec<PacketByteBuf, CyclopsTradesPayload> CODEC = PacketCodec.of(
        (value, buf) -> {
            buf.writeVarInt(value.syncId);
            buf.writeString(value.npcId);
            buf.writeString(value.npcName);
            buf.writeBoolean(value.hasBuyMode);
            buf.writeBoolean(value.hasSellMode);
            buf.writeCollection(value.trades, (b, t) -> {
                ItemStack.PACKET_CODEC.encode((RegistryByteBuf) b, t.input());
                ItemStack.PACKET_CODEC.encode((RegistryByteBuf) b, t.output());
                b.writeVarInt(t.originalIndex());
            });
        },
        buf -> {
            int syncId = buf.readVarInt();
            String npcId = buf.readString();
            String npcName = buf.readString();
            boolean hasBuyMode = buf.readBoolean();
            boolean hasSellMode = buf.readBoolean();
            List<TradeData> trades = buf.readList(b -> new TradeData(
                ItemStack.PACKET_CODEC.decode((RegistryByteBuf) b),
                ItemStack.PACKET_CODEC.decode((RegistryByteBuf) b),
                b.readVarInt()
            ));
            return new CyclopsTradesPayload(syncId, npcId, npcName, hasBuyMode, hasSellMode, trades);
        }
    );
    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
