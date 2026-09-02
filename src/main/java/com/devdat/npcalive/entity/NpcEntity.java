package com.devdat.npcalive.entity;

import com.devdat.npcalive.entity.ia.FollowPlayerGoal;
import com.devdat.npcalive.network.OpenNpcGuiPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
    private int interactionPauseTimer = 0;

    private final net.minecraft.world.SimpleContainer inventory = new net.minecraft.world.SimpleContainer(27);

    private static final EntityDataAccessor<Integer> DATA_ROMANCE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_NPC_TITLE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_NPC_VARIANT = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_NPC_GENDER = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FRIENDSHIP = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_NPC_BEHAVIOR = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_IS_MARRIED =
            net.minecraft.network.syncher.SynchedEntityData.defineId(NpcEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    public enum NpcBehavior {
        WANDER,
        FOLLOW,
        STAY;

        public NpcBehavior next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
    }
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
        // Permitir que el navegador considere las puertas como caminos válidos
        if (this.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation groundNav) {
            groundNav.setCanOpenDoors(true);
        }
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
        this.goalSelector.addGoal(1, new FollowPlayerGoal(this, 1.2D, 4.0F, 16.0F)); // Activo solo si está en FOLLOW
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this, true)); // <-- Permite abrir y cerrar puertas

        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D) {
            @Override
            public boolean canUse() {
                return !NpcEntity.this.isInteracting() && NpcEntity.this.getBehavior() == NpcBehavior.WANDER && super.canUse();
            }
        });

        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D) {
            @Override
            public boolean canUse() {
                return NpcEntity.this.getBehavior() == NpcBehavior.WANDER && super.canUse();
            }
        });
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.interactionPauseTimer > 0) {
            this.interactionPauseTimer--;
            this.getNavigation().stop();
        }
    }

    public boolean isInteracting() {
        return this.interactionPauseTimer > 0;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            this.interactionPauseTimer = 100; // Pausa por 5 segundos
            this.getNavigation().stop();
            this.getLookControl().setLookAt(player, 30.0F, 30.0F);

            int entityId = this.getId();
            String npcTitle = this.getNpcTitle();
            String genderStr = (this.getNpcGender() == NpcGender.MALE) ? "male" : "female";

            serverPlayer.connection.send(new OpenNpcGuiPacket(entityId, npcTitle, genderStr));

            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
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
        builder.define(DATA_FRIENDSHIP, 0);
        builder.define(DATA_NPC_BEHAVIOR, NpcBehavior.WANDER.ordinal()); // Empieza vagando por defecto
        builder.define(DATA_ROMANCE, 0);
        builder.define(DATA_IS_MARRIED, false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("NpcTitle", this.getNpcTitle());
        output.putInt("NpcVariant", this.getNpcVariant());
        output.putInt("NpcGender", this.getNpcGenderOrdinal());
        output.putInt("Friendship", this.getFriendship());
        output.putInt("NpcBehavior", this.getBehavior().ordinal());
        output.putInt("Romance", this.getRomance());
        output.putBoolean("IsMarried", this.isMarried());

        // Guardar la mochila (ValueOutput.child es directo, sin ifPresent)
        net.minecraft.world.ContainerHelper.saveAllItems(output.child("NpcInventory"), this.inventory.getItems());

        // Guardar el equipamiento visual en una NonNullList de 6 elementos
        net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> equipmentList =
                net.minecraft.core.NonNullList.withSize(6, net.minecraft.world.item.ItemStack.EMPTY);
        equipmentList.set(0, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD));
        equipmentList.set(1, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST));
        equipmentList.set(2, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS));
        equipmentList.set(3, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET));
        equipmentList.set(4, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
        equipmentList.set(5, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND));

        net.minecraft.world.ContainerHelper.saveAllItems(output.child("NpcEquipment"), equipmentList);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.getString("NpcTitle").ifPresent(this::setNpcTitle);
        input.getInt("NpcVariant").ifPresent(this::setNpcVariant);
        input.getInt("NpcGender").ifPresent(ordinal -> this.setNpcGender(NpcGender.values()[ordinal]));
        input.getInt("Friendship").ifPresent(this::setFriendship);
        input.getInt("NpcBehavior").ifPresent(ordinal -> {
            NpcBehavior[] behaviors = NpcBehavior.values();
            if (ordinal >= 0 && ordinal < behaviors.length) {
                this.setBehavior(behaviors[ordinal]);
            }
        });
        input.getInt("Romance").ifPresent(this::setRomance);
        this.setMarried(input.getBooleanOr("IsMarried", false));

        // Cargar la mochila (ValueInput sí devuelve Optional, por lo que lleva ifPresent)
        input.child("NpcInventory").ifPresent(childInput -> {
            net.minecraft.world.ContainerHelper.loadAllItems(childInput, this.inventory.getItems());
        });

        // Cargar el equipamiento
        input.child("NpcEquipment").ifPresent(childInput -> {
            net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> equipmentList =
                    net.minecraft.core.NonNullList.withSize(6, net.minecraft.world.item.ItemStack.EMPTY);
            net.minecraft.world.ContainerHelper.loadAllItems(childInput, equipmentList);

            this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, equipmentList.get(0));
            this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, equipmentList.get(1));
            this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, equipmentList.get(2));
            this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, equipmentList.get(3));
            this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, equipmentList.get(4));
            this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, equipmentList.get(5));
        });
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

    public int getFriendship() {
        return this.entityData.get(DATA_FRIENDSHIP);
    }

    public void setFriendship(int friendship) {
        this.entityData.set(DATA_FRIENDSHIP, friendship);
    }

    public void addFriendship(int amount) {
        this.setFriendship(net.minecraft.util.Mth.clamp(this.getFriendship() + amount, -250, 250));
    }

    public NpcBehavior getBehavior() {
        int ordinal = this.entityData.get(DATA_NPC_BEHAVIOR);
        NpcBehavior[] values = NpcBehavior.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NpcBehavior.WANDER;
    }

    public void setBehavior(NpcBehavior behavior) {
        this.entityData.set(DATA_NPC_BEHAVIOR, behavior.ordinal());
    }

    public int getRomance() {
        return this.entityData.get(DATA_ROMANCE);
    }

    public void setRomance(int romance) {
        this.entityData.set(DATA_ROMANCE, romance);
    }

    public void addRomance(int amount) {
        this.setRomance(net.minecraft.util.Mth.clamp(this.getRomance() + amount, -250, 250));
    }

    public String getDialogueKey() {
        if (this.getRomance() >= 50) {
            return "dialog.npcalive.mood.romantic";
        } else if (this.getFriendship() < 0 || this.getRomance() < -10) {
            return "dialog.npcalive.mood.hostile";
        } else {
            return "dialog.npcalive.mood.neutral";
        }
    }

    public boolean isMarried() {
        return this.entityData.get(DATA_IS_MARRIED);
    }

    public void setMarried(boolean married) {
        this.entityData.set(DATA_IS_MARRIED, married);
    }

    public net.minecraft.world.SimpleContainer getInventory() {
        return this.inventory;
    }

    @Override
    public net.minecraft.world.item.ItemStack getItemBySlot(net.minecraft.world.entity.EquipmentSlot slot) {
        // Mapeamos ranuras fijas de la mochila (SimpleContainer) al equipo del NPC
        int targetSlot = switch (slot) {
            case MAINHAND -> 0; // Ranura 0 de la mochila es la mano principal
            case HEAD     -> 1; // Ranura 1 es el casco
            case CHEST    -> 2; // Ranura 2 es la pechera
            case LEGS     -> 3; // Ranura 3 es las perneras
            case FEET     -> 4; // Ranura 4 son las botas
            case OFFHAND  -> 5; // Ranura 5 es la mano secundaria
            default -> -1;
        };

        if (targetSlot >= 0 && targetSlot < this.inventory.getContainerSize()) {
            return this.inventory.getItem(targetSlot);
        }
        return super.getItemBySlot(slot);
    }

    @Override
    public void setItemSlot(net.minecraft.world.entity.EquipmentSlot slot, net.minecraft.world.item.ItemStack stack) {
        int targetSlot = switch (slot) {
            case MAINHAND -> 0;
            case HEAD     -> 1;
            case CHEST    -> 2;
            case LEGS     -> 3;
            case FEET     -> 4;
            case OFFHAND  -> 5;
            default -> -1;
        };

        if (targetSlot >= 0 && targetSlot < this.inventory.getContainerSize()) {
            this.inventory.setItem(targetSlot, stack);
        } else {
            super.setItemSlot(slot, stack);
        }
    }
}