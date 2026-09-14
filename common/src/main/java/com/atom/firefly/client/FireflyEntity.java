package com.atom.firefly.client;

import com.atom.firefly.Constants;
import com.atom.firefly.config.FireflyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

public class FireflyEntity extends Entity {

    private int age = 0;
    private final int lifetime;
    private BlockPos homePos = null;
    private BlockPos lastLightPos = null;
    private FireflyVariant variant = FireflyVariant.FOREST;

    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

    private double vx, vy, vz;
    private double targetX, targetY, targetZ;

    public FireflyEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.lifetime = 600 + level.random.nextInt(600);
    }

    public FireflyVariant getVariant() {
        return this.variant;
    }

    public void setVariant(FireflyVariant variant) {
        this.variant = variant;
    }

    private void pickNewTarget() {
        if (this.homePos == null) return;

        for (int i = 0; i < 5; i++) {
            double proposedX = this.homePos.getX() + (this.random.nextDouble() - 0.5) * 8.0;
            double proposedZ = this.homePos.getZ() + (this.random.nextDouble() - 0.5) * 8.0;

            this.mutablePos.set(proposedX, this.getY() + 2.0, proposedZ);
            boolean foundGround = false;

            for (int j = 0; j < 6; j++) {
                if (!this.level().getBlockState(this.mutablePos).isAir()) {
                    foundGround = true;
                    break;
                }
                this.mutablePos.move(0, -1, 0);
            }

            if (foundGround) {
                double groundY = this.mutablePos.getY() + 1.0;
                double proposedY = groundY + 0.5 + (this.random.nextDouble() * 3.0);

                this.mutablePos.set(proposedX, proposedY, proposedZ);
                BlockState targetState = this.level().getBlockState(this.mutablePos);

                if (targetState.isAir() && targetState.getFluidState().isEmpty()) {
                    this.targetX = proposedX;
                    this.targetY = proposedY;
                    this.targetZ = proposedZ;
                    return;
                }
            }
        }

        this.targetX = this.homePos.getX();
        this.targetY = this.getY() - 1.0;
        this.targetZ = this.homePos.getZ();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.homePos == null) {
            this.homePos = this.blockPosition();
            this.variant = FireflyVariant.fromBiome(this.level().getBiome(this.homePos));
            this.pickNewTarget();
        }

        this.age++;
        this.yRotO = this.getYRot();

        if (this.age > this.lifetime) {
            this.discard();
            return;
        }

        // Despawn safely if no player is nearby (uses Level API, no direct Minecraft.getInstance())
        if (this.age % 40 == 0) {
            if (this.level().getNearestPlayer(this, 64.0D) == null) {
                this.discard();
                return;
            }
        }

        // Vertical obstacle and water checking
        if (this.age % 4 == 0) {
            if (this.isInWater()) {
                this.vy += 0.05;
            } else {
                this.mutablePos.set(this.getX(), this.getY() - 1.0, this.getZ());

                if (!this.level().getBlockState(this.mutablePos).isAir()) {
                    this.vy += 0.03;
                } else {
                    boolean isTooHigh = true;
                    for (int i = 0; i < 3; i++) {
                        this.mutablePos.move(0, -1, 0);
                        if (!this.level().getBlockState(this.mutablePos).isAir()) {
                            isTooHigh = false;
                            break;
                        }
                    }

                    if (isTooHigh) {
                        this.vy -= 0.03;
                        if (this.targetY > this.getY()) {
                            this.targetY = this.getY() - 1.0;
                        }
                    }
                }
            }
        }

        double dx = this.targetX - this.getX();
        double dy = this.targetY - this.getY();
        double dz = this.targetZ - this.getZ();

        double distSqr = dx * dx + dy * dy + dz * dz;

        if (distSqr < 1.0 || this.age % 80 == 0) {
            this.pickNewTarget();
        } else if (distSqr > 0.001) {
            double distance = Math.sqrt(distSqr);
            double speed = 0.04;
            this.vx += (dx / distance * speed - this.vx) * 0.1;
            this.vy += (dy / distance * speed - this.vy) * 0.1;
            this.vz += (dz / distance * speed - this.vz) * 0.1;
        }

        double hover = Math.sin(this.age * 0.15) * 0.015;
        this.setPos(this.getX() + this.vx, this.getY() + this.vy + hover, this.getZ() + this.vz);

        float targetYaw = (float) (Mth.atan2(this.vz, this.vx) * (180F / Math.PI)) + 90.0F;
        float smoothYaw = Mth.approachDegrees(this.getYRot(), targetYaw, 10.0F);
        this.setYRot(smoothYaw);

        // Ambient dynamic light on the ground and surroundings (controlled by config)
        if (this.age % 2 == 0) {
            this.updateDynamicLight();
        }

        // Ambient glow particle effect (controlled by persistent config)
        if (FireflyConfig.get().enableParticles && this.level().isClientSide() && this.random.nextFloat() < 0.05F) {
            double px = this.getX() + (this.random.nextDouble() - 0.5) * 0.1;
            double py = this.getY() + (this.random.nextDouble() - 0.5) * 0.1 + 0.1;
            double pz = this.getZ() + (this.random.nextDouble() - 0.5) * 0.1;

            this.level().addParticle(
                    ParticleTypes.GLOW,
                    px, py, pz,
                    0.0D, 0.0D, 0.0D
            );
        }
    }

    private void updateDynamicLight() {
        if (!FireflyConfig.get().enableDynamicLight) {
            this.clearDynamicLight();
            return;
        }

        BlockPos currentPos = this.blockPosition();
        if (currentPos.equals(this.lastLightPos)) {
            return;
        }

        this.clearDynamicLight();

        if (this.level().getBlockState(currentPos).isAir()) {
            this.level().setBlock(currentPos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 8), 3);
            this.lastLightPos = currentPos.immutable();
        }
    }

    private void clearDynamicLight() {
        if (this.lastLightPos != null) {
            if (this.level().getBlockState(this.lastLightPos).is(Blocks.LIGHT)) {
                this.level().setBlock(this.lastLightPos, Blocks.AIR.defaultBlockState(), 3);
            }
            this.lastLightPos = null;
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public void remove(RemovalReason reason) {
        this.clearDynamicLight();
        super.remove(reason);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return super.interact(player, hand);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        return false;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}