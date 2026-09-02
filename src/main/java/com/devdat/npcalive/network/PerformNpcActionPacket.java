package com.devdat.npcalive.network;

import com.devdat.npcalive.NpcAlive;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PerformNpcActionPacket(int entityId, String actionName) implements CustomPacketPayload {
    public static final Type<PerformNpcActionPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(NpcAlive.MOD_ID, "perform_npc_action"));

    public static final StreamCodec<ByteBuf, PerformNpcActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PerformNpcActionPacket::entityId,
            ByteBufCodecs.STRING_UTF8, PerformNpcActionPacket::actionName,
            PerformNpcActionPacket::new
    );

    @Override
    public Type<PerformNpcActionPacket> type() {
        return TYPE;
    }
}