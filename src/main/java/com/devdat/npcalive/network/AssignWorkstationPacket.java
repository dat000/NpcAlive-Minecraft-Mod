package com.devdat.npcalive.network;

import com.devdat.npcalive.NpcAlive;
import com.devdat.npcalive.entity.NpcEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AssignWorkstationPacket(int entityId) implements CustomPacketPayload {
    public static final Type<AssignWorkstationPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(NpcAlive.MOD_ID, "assign_workstation"));

    public static final StreamCodec<ByteBuf, AssignWorkstationPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AssignWorkstationPacket::entityId,
            AssignWorkstationPacket::new
    );

    @Override
    public Type<AssignWorkstationPacket> type() {
        return TYPE;
    }

    public static void handle(AssignWorkstationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.literal("DEBUG: Paquete recibido en el servidor."));

                Level level = serverPlayer.level();
                Entity entity = level.getEntity(packet.entityId());

                if (entity instanceof NpcEntity npc) {
                    serverPlayer.sendSystemMessage(Component.literal("DEBUG: NPC encontrado. ¿Es familia? " + npc.isFamily()));
                    serverPlayer.sendSystemMessage(Component.literal("DEBUG: Profesión actual -> " + npc.getProfession()));
                    if (npc.isFamily()) {
                        boolean success = npc.searchAndAssignWorkstation();
                        if (success) {
                            serverPlayer.sendSystemMessage(Component.literal("¡Estación de trabajo asignada con éxito a " + npc.getNpcTitle() + "!"));
                        } else {
                            serverPlayer.sendSystemMessage(Component.literal("No se encontró ningún bloque de trabajo compatible cercano."));
                        }
                    } else {
                        serverPlayer.sendSystemMessage(Component.literal("DEBUG: El NPC NO es familia, acción bloqueada."));
                    }
                } else {
                    serverPlayer.sendSystemMessage(Component.literal("DEBUG: La entidad no es un NpcEntity o el ID es inválido."));
                }
            }
        });
    }
}