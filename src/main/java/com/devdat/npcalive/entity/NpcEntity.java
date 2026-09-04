package com.devdat.npcalive.entity;

import com.devdat.npcalive.entity.ia.FollowPlayerGoal;
import com.devdat.npcalive.entity.ia.ReturnHomeGoal;
import com.devdat.npcalive.entity.ia.SleepInBedGoal;
import com.devdat.npcalive.entity.ia.WorkAtStationGoal;
import com.devdat.npcalive.network.OpenNpcGuiPacket;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;

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
    private BlockPos bedPosition = null;
    private net.minecraft.core.BlockPos homePosition = null;
    private net.minecraft.core.BlockPos workPosition = null;
    private ItemStack savedMainHandItem = ItemStack.EMPTY;
    private boolean hasEquippedWorkTool = false;

    private final net.minecraft.world.SimpleContainer inventory = new net.minecraft.world.SimpleContainer(27);
    private static final EntityDataAccessor<Integer> DATA_ROMANCE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_NPC_TITLE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_NPC_VARIANT = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_NPC_GENDER = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FRIENDSHIP = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_NPC_BEHAVIOR = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_PROFESSION = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_IS_FAMILY = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BOOLEAN);
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

        // Configuración completa para que el navegador maneje puertas correctamente
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
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this, true));

        // Prioridad 2: El trabajo es la máxima prioridad durante el horario laboral
        this.goalSelector.addGoal(2, new WorkAtStationGoal(this, 1.0D));

        // Prioridad 3: Volver a casa inmediatamente al terminar el turno
        this.goalSelector.addGoal(3, new ReturnHomeGoal(this, 1.0D));

        // Prioridad 4: Dormir si es de noche
        this.goalSelector.addGoal(4, new SleepInBedGoal(this, 1.0D));

        // Prioridad 5: Seguir al jugador si se le solicita
        this.goalSelector.addGoal(5, new FollowPlayerGoal(this, 1.2D, 4.0F, 16.0F));

        // Prioridad 6: Acciones pasivas de atención
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // Prioridad 7: Deambular (Último recurso cuando está libre)
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0D) {
            @Override
            public boolean canUse() {
                if (NpcEntity.this.isInteracting() || NpcEntity.this.getBehavior() != NpcBehavior.WANDER) {
                    return false;
                }

                if (NpcEntity.this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    long timeOfDay = serverLevel.getDefaultClockTime() % 24000;
                    boolean isNight = timeOfDay >= 13000 && timeOfDay < 23000;

                    if (isNight && NpcEntity.this.getHomePos() != null) {
                        return NpcEntity.this.blockPosition().distSqr(NpcEntity.this.getHomePos()) <= 36.0D && super.canUse();
                    }
                }

                return super.canUse();
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

        if (!this.isFamily()) {
            this.setProfession(NpcProfession.randomProfession(this.random));
        } else {
            this.setProfession(NpcProfession.NONE); // Los de familia empiezan sin profesión asignada hasta que el jugador decida
        }

        return spawnGroupData;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_NPC_TITLE, "");
        builder.define(DATA_NPC_VARIANT, 0);
        builder.define(DATA_NPC_GENDER, 0);
        builder.define(DATA_FRIENDSHIP, 0);
        builder.define(DATA_NPC_BEHAVIOR, NpcBehavior.WANDER.ordinal());
        builder.define(DATA_ROMANCE, 0);
        builder.define(DATA_IS_MARRIED, false);
        builder.define(DATA_PROFESSION, "NONE"); // Nuevo
        builder.define(DATA_IS_FAMILY, false);   // Nuevo
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
        output.putBoolean("IsFamily", this.isFamily()); // Usando el método sincronizado
        output.putString("NpcProfession", this.getProfession().name()); // Guardamos la profesión como texto

        // Guardar la mochila
        net.minecraft.world.ContainerHelper.saveAllItems(output.child("NpcInventory"), this.inventory.getItems());

        // Guardar el equipamiento visual
        net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> equipmentList =
                net.minecraft.core.NonNullList.withSize(6, net.minecraft.world.item.ItemStack.EMPTY);
        equipmentList.set(0, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD));
        equipmentList.set(1, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST));
        equipmentList.set(2, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS));
        equipmentList.set(3, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET));
        equipmentList.set(4, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
        equipmentList.set(5, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND));

        net.minecraft.world.ContainerHelper.saveAllItems(output.child("NpcEquipment"), equipmentList);

        if (this.homePosition != null) {
            output.putBoolean("HasHome", true);
            output.putInt("HomeX", this.homePosition.getX());
            output.putInt("HomeY", this.homePosition.getY());
            output.putInt("HomeZ", this.homePosition.getZ());
        } else {
            output.putBoolean("HasHome", false);
        }

        if (this.bedPosition != null) {
            output.putBoolean("HasBed", true);
            output.putInt("BedX", this.bedPosition.getX());
            output.putInt("BedY", this.bedPosition.getY());
            output.putInt("BedZ", this.bedPosition.getZ());
        } else {
            output.putBoolean("HasBed", false);
        }

        if (this.workPosition != null) {
            output.putBoolean("HasWork", true);
            output.putInt("WorkX", this.workPosition.getX());
            output.putInt("WorkY", this.workPosition.getY());
            output.putInt("WorkZ", this.workPosition.getZ());
        } else {
            output.putBoolean("HasWork", false);
        }
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

        // Cargar la mochila
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

        if (input.getBooleanOr("HasHome", false)) {
            input.getInt("HomeX").ifPresent(x -> {
                input.getInt("HomeY").ifPresent(y -> {
                    input.getInt("HomeZ").ifPresent(z -> {
                        this.setHomePos(new net.minecraft.core.BlockPos(x, y, z));
                    });
                });
            });
        }

        if (input.getBooleanOr("HasBed", false)) {
            input.getInt("BedX").ifPresent(x -> {
                input.getInt("BedY").ifPresent(y -> {
                    input.getInt("BedZ").ifPresent(z -> {
                        this.setBedPos(new net.minecraft.core.BlockPos(x, y, z));
                    });
                });
            });
        }

        this.setFamily(input.getBooleanOr("IsFamily", false));

        // Carga robusta de profesión (soporta texto nuevo o respaldo numérico antiguo)
        input.getString("NpcProfession").ifPresentOrElse(name -> {
            try {
                this.setProfession(NpcProfession.valueOf(name));
            } catch (IllegalArgumentException e) {
                this.setProfession(NpcProfession.NONE);
            }
        }, () -> {
            input.getInt("NpcProfession").ifPresent(ordinal -> {
                NpcProfession[] profs = NpcProfession.values();
                if (ordinal >= 0 && ordinal < profs.length) {
                    this.setProfession(profs[ordinal]);
                }
            });
        });

        if (input.getBooleanOr("HasWork", false)) {
            input.getInt("WorkX").ifPresent(x -> {
                input.getInt("WorkY").ifPresent(y -> {
                    input.getInt("WorkZ").ifPresent(z -> {
                        this.setWorkPos(new net.minecraft.core.BlockPos(x, y, z));
                    });
                });
            });
        }
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

    public net.minecraft.core.BlockPos getHomePos() {
        return this.homePosition;
    }

    public void setHomePos(net.minecraft.core.BlockPos pos) {
        this.homePosition = pos;
        // Nota: La distancia máxima y la orden de "volver a casa"
        // las vamos a programar nosotros mismos en un Goal (IA) en el siguiente paso.
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

    public net.minecraft.core.BlockPos getBedPos() {
        return this.bedPosition;
    }

    public void setBedPos(net.minecraft.core.BlockPos pos) {
        this.bedPosition = pos;
    }

    @Override
    public boolean isPushable() {
        // Si el NPC está durmiendo, desactivamos la capacidad de ser empujado
        return !this.isSleeping() && super.isPushable();
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
        // Evita que el NPC se desplace al chocar con otras entidades mientras duerme
        if (!this.isSleeping()) {
            super.doPush(entity);
        }
    }

    public boolean isFamily() {
        return this.entityData.get(DATA_IS_FAMILY);
    }

    public void setFamily(boolean family) {
        this.entityData.set(DATA_IS_FAMILY, family);
    }

    // Métodos de acceso
    public NpcProfession getProfession() {
        try {
            return NpcProfession.valueOf(this.entityData.get(DATA_PROFESSION));
        } catch (Exception e) {
            return NpcProfession.NONE;
        }
    }

    public void setProfession(NpcProfession profession) {
        this.entityData.set(DATA_PROFESSION, profession != null ? profession.name() : "NONE");
    }

    public net.minecraft.core.BlockPos getWorkPos() {
        return this.workPosition;
    }

    public void setWorkPos(net.minecraft.core.BlockPos pos) {
        this.workPosition = pos;
    }

    public boolean isValidWorkstation(net.minecraft.world.level.block.state.BlockState state) {
        return this.getProfession().matchesWorkstation(state);
    }

    public boolean searchAndAssignWorkstation() {
        BlockPos currentPos = this.blockPosition();
        int radius = 16; // Ampliamos un poco el radio para pruebas
        int verticalRange = 6; // Ampliamos de 3 a 6 bloques hacia arriba y abajo

        System.out.println("[NPC Debug] " + this.getNpcTitle() + " buscando estación de trabajo alrededor de: " + currentPos);

        for (BlockPos bp : BlockPos.betweenClosed(
                currentPos.offset(-radius, -verticalRange, -radius),
                currentPos.offset(radius, verticalRange, radius))) {

            BlockState state = this.level().getBlockState(bp);

            // Verificamos si el bloque coincide con alguna profesión
            NpcProfession foundProf = NpcProfession.getByWorkstation(state);

            if (foundProf != NpcProfession.NONE) {
                System.out.println("[NPC Debug] ¡Bloque de trabajo encontrado!: " + state.getBlock().getName().getString() + " en " + bp);

                // Verificamos que el bloque no esté ocupado por otro NPC
                if (!isWorkstationTaken(bp.immutable())) {
                    this.setProfession(foundProf);
                    this.setWorkPos(bp.immutable());
                    System.out.println("[NPC Debug] ¡Estación asignada con éxito! Profesión: " + foundProf.name());
                    return true;
                } else {
                    System.out.println("[NPC Debug] El bloque en " + bp + " ya está ocupado por otro NPC.");
                }
            }
        }

        System.out.println("[NPC Debug] No se encontró ninguna estación de trabajo libre en el rango.");
        return false;
    }

    private boolean isBlockCompatibleWithProfession(BlockPos pos) {
        if (this.level() == null) return false;
        net.minecraft.world.level.block.state.BlockState state = this.level().getBlockState(pos);

        // Aprovecha directamente el metodo que ya tiene tu enum NpcProfession
        return this.getProfession().matchesWorkstation(state);
    }

    private boolean isWorkstationTaken(BlockPos pos) {
        if (this.level() == null) return false;

        net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(pos).inflate(16.0D);
        for (NpcEntity otherNpc : this.level().getEntitiesOfClass(NpcEntity.class, searchBox)) {
            if (otherNpc != this && otherNpc.getWorkPos() != null && otherNpc.getWorkPos().equals(pos)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBedTaken(BlockPos headPos) {
        if (this.level() == null) return false;

        // Ampliamos la búsqueda alrededor del NPC para detectar si otro NPC cercano ya reclama esta cama
        net.minecraft.world.phys.AABB searchBox = this.getBoundingBox().inflate(32.0D);
        for (NpcEntity otherNpc : this.level().getEntitiesOfClass(NpcEntity.class, searchBox)) {
            if (otherNpc != this) {
                // Verificar si tiene la cama asignada en su memoria
                if (otherNpc.getBedPos() != null && otherNpc.getBedPos().equals(headPos)) {
                    return true;
                }
                // O si ya está acostado físicamente en esa posición
                if (otherNpc.isSleeping() && headPos.equals(otherNpc.blockPosition())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean searchAndAssignBed() {
        BlockPos currentPos = this.blockPosition();
        int radius = 12; // Radio de búsqueda de camas

        for (BlockPos bp : BlockPos.betweenClosed(
                currentPos.offset(-radius, -3, -radius),
                currentPos.offset(radius, 3, radius))) {

            BlockState state = this.level().getBlockState(bp);
            if (state.getBlock() instanceof BedBlock) {
                BlockPos headPos = bp;
                if (state.getValue(BedBlock.PART) != BedPart.HEAD) {
                    net.minecraft.core.Direction facing = state.getValue(BedBlock.FACING);
                    headPos = bp.relative(facing);
                }

                if (!isBedTaken(headPos.immutable())) {
                    this.setBedPos(headPos.immutable());
                    return true;
                }
            }
        }
        return false;
    }

    public BlockPos getValidWorkPos() {
        if (this.workPosition == null) return null;

        BlockState state = this.level().getBlockState(this.workPosition);
        // Comprobamos si el bloque actual sigue coincidiendo con la profesión del NPC
        if (!NpcProfession.getByWorkstation(state).equals(this.getProfession())) {
            // ¡El bloque fue destruido o cambiado! Borramos la memoria
            this.workPosition = null;
            this.setProfession(NpcProfession.NONE);
            return null;
        }
        return this.workPosition;
    }

    public void equipWorkTool(ItemStack workTool) {
        if (!hasEquippedWorkTool && !workTool.isEmpty()) {
            // 1. Guardamos lo que tenía en la mano ANTES de empezar a trabajar
            this.savedMainHandItem = this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND).copy();
            // 2. Le ponemos la herramienta de trabajo en la mano
            this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, workTool.copy());
            hasEquippedWorkTool = true;
        }
    }

    public void restoreOriginalHand() {
        if (hasEquippedWorkTool) {
            // 3. Al terminar, devolvemos exactamente lo que tenía equipado
            this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, this.savedMainHandItem);
            this.savedMainHandItem = ItemStack.EMPTY;
            hasEquippedWorkTool = false;
        }
    }

}