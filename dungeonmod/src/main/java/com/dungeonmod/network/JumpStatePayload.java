package com.dungeonmod.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Envoyé par le client quand l'état de la touche SAUT (espace) change.
 * Permet au serveur de savoir si le joueur maintient espace (cape du voyageur).
 */
public record JumpStatePayload(boolean jumping) implements CustomPayload {
    public static final CustomPayload.Id<JumpStatePayload> ID = new CustomPayload.Id<>(Identifier.of("dungeonmod", "jump_state"));
    public static final PacketCodec<PacketByteBuf, JumpStatePayload> CODEC = PacketCodec.of(
        (value, buf) -> buf.writeBoolean(value.jumping),
        buf -> new JumpStatePayload(buf.readBoolean())
    );
    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
