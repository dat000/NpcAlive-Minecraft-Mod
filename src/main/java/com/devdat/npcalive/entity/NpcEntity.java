package com.devdat.npcalive.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class NpcEntity extends Monster {

    public NpcEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }


    // SISTEMA IA NPCS - REGISTRO DE OBJETIVOS //
    @Override
    protected void registerGoals() {
        // Prioridad 0: Evitar el agua al deambular y caminar de forma aleatoria
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0D));

        // Prioridad 1: Mirar a los jugadores cercanos
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 8.0F));

        // Prioridad 2: Mirar alrededor de vez en cuando de forma aleatoria
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }
    // ------------------------------------------- //
}