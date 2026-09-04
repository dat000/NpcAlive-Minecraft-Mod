package com.devdat.npcalive.network;

import com.devdat.npcalive.NpcAlive;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SetNpcHomePacket(int entityId) implements CustomPacketPayload {

    public static final Type<SetNpcHomePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(NpcAlive.MOD_ID, "set_npc_home"));

    public static final StreamCodec<ByteBuf, SetNpcHomePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetNpcHomePacket::entityId,
            SetNpcHomePacket::new
    );

    @Override
    public Type<SetNpcHomePacket> type() {
        return TYPE;
    }
}