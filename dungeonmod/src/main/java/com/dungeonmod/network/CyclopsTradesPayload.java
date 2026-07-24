package com.dungeonmod.network;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

public record CyclopsTradesPayload(int syncId, List<TradeData> trades) implements CustomPayload {
    public static final CustomPayload.Id<CyclopsTradesPayload> ID = new CustomPayload.Id<>(Identifier.of("dungeonmod", "cyclops_trades"));
    public static final PacketCodec<PacketByteBuf, CyclopsTradesPayload> CODEC = PacketCodec.of(
        (value, buf) -> {
            buf.writeVarInt(value.syncId);
            buf.writeCollection(value.trades, (b, t) -> {
                ItemStack.PACKET_CODEC.encode((RegistryByteBuf) b, t.input());
                ItemStack.PACKET_CODEC.encode((RegistryByteBuf) b, t.output());
                b.writeVarInt(t.originalIndex());
            });
        },
        buf -> {
            int syncId = buf.readVarInt();
            List<TradeData> trades = buf.readList(b -> new TradeData(
                ItemStack.PACKET_CODEC.decode((RegistryByteBuf) b),
                ItemStack.PACKET_CODEC.decode((RegistryByteBuf) b),
                b.readVarInt()
            ));
            return new CyclopsTradesPayload(syncId, trades);
        }
    );
    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
