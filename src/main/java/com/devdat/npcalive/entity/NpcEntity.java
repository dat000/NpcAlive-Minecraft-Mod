package com.devdat.npcalive.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// Imports de IA
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;

// Imports para el nacimiento (finalizeSpawn)
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import org.jetbrains.annotations.Nullable;

public class NpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> DATA_NPC_TITLE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_NPC_VARIANT = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_NPC_GENDER = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);

    public enum NpcGender {
        MALE,
        FEMALE;

        public static NpcGender randomGender(net.minecraft.util.RandomSource random) {
            return random.nextBoolean() ? MALE : FEMALE;
        }
    }

    public NpcEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData spawnGroupData) {
        spawnGroupData = super.finalizeSpawn(level, difficulty, spawnReason, spawnGroupData);

        // 1. Asignar género aleatorio
        NpcGender generoElegido = NpcGender.randomGender(this.random);
        this.setNpcGender(generoElegido);

        // 2. Nombres según el género
        String[] nombresMasculinos = {"Juan", "Carlos", "Pedro"};
        String[] nombresFemeninos = {"María", "Ana", "Sofía"};

        String nombreElegido = (generoElegido == NpcGender.MALE)
                ? nombresMasculinos[this.random.nextInt(nombresMasculinos.length)]
                : nombresFemeninos[this.random.nextInt(nombresFemeninos.length)];

        this.setNpcTitle(nombreElegido);

        // 3. Variante visual aleatoria
        int varianteAleatoria = this.random.nextInt(4);
        this.setNpcVariant(varianteAleatoria);

        return spawnGroupData;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_NPC_TITLE, "");
        builder.define(DATA_NPC_VARIANT, 0);
        builder.define(DATA_NPC_GENDER, 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("NpcTitle", this.getNpcTitle());
        output.putInt("NpcVariant", this.getNpcVariant());
        output.putInt("NpcGender", this.getNpcGenderOrdinal());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.getString("NpcTitle").ifPresent(this::setNpcTitle);
        input.getInt("NpcVariant").ifPresent(this::setNpcVariant);
        input.getInt("NpcGender").ifPresent(ordinal -> this.setNpcGender(NpcGender.values()[ordinal]));
    }

    public String getNpcTitle() {
        return this.entityData.get(DATA_NPC_TITLE);
    }

    public void setNpcTitle(String title) {
        this.entityData.set(DATA_NPC_TITLE, title);
        this.setCustomName(Component.literal(title));
    }

    public int getNpcVariant() {
        return this.entityData.get(DATA_NPC_VARIANT);
    }

    public void setNpcVariant(int variant) {
        this.entityData.set(DATA_NPC_VARIANT, variant);
    }

    public NpcGender getNpcGender() {
        int ordinal = this.entityData.get(DATA_NPC_GENDER);
        return NpcGender.values()[ordinal >= 0 && ordinal < NpcGender.values().length ? ordinal : 0];
    }

    public int getNpcGenderOrdinal() {
        return this.getNpcGender().ordinal();
    }

    public void setNpcGender(NpcGender gender) {
        this.entityData.set(DATA_NPC_GENDER, gender.ordinal());
    }
}