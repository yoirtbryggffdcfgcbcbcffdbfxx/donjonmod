package com.dungeonmod.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record SubtitlePayload(String speakerName, List<String> lines, boolean canOpenShop) implements CustomPayload {
    public static final CustomPayload.Id<SubtitlePayload> ID = new CustomPayload.Id<>(Identifier.of("dungeonmod", "subtitle"));

    public static final PacketCodec<PacketByteBuf, SubtitlePayload> CODEC = PacketCodec.of(
        (value, buf) -> {
            buf.writeString(value.speakerName);
            buf.writeVarInt(value.lines.size());
            for (String l : value.lines) buf.writeString(l);
            buf.writeBoolean(value.canOpenShop);
        },
        buf -> {
            String n = buf.readString();
            int s = buf.readVarInt();
            List<String> ls = new ArrayList<>();
            for (int i = 0; i < s; i++) ls.add(buf.readString());
            boolean shop = buf.readBoolean();
            return new SubtitlePayload(n, ls, shop);
        }
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
