package com.devdat.npcalive.network;

import com.devdat.npcalive.NpcAlive;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenNpcGuiPacket(int entityId, String npcTitle, String genderStr) implements CustomPacketPayload {
    public static final Type<OpenNpcGuiPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(NpcAlive.MOD_ID, "open_npc_gui"));

    public static final StreamCodec<ByteBuf, OpenNpcGuiPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenNpcGuiPacket::entityId,
            ByteBufCodecs.STRING_UTF8, OpenNpcGuiPacket::npcTitle,
            ByteBufCodecs.STRING_UTF8, OpenNpcGuiPacket::genderStr,
            OpenNpcGuiPacket::new
    );

    @Override
    public Type<OpenNpcGuiPacket> type() {
        return TYPE;
    }
}