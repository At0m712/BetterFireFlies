package com.atom.firefly.client;

import com.atom.firefly.Constants;
import com.atom.firefly.config.FireflyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class FireflyEntity extends Entity {

    private int age = 0;
    private final int lifetime;
    private BlockPos homePos = null;
    private BlockPos lastLightPos = null;
    private FireflyVariant variant = FireflyVariant.FOREST;

    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

    private double vx, vy, vz;
    private double targetX, targetY, targetZ;
    private int hoverTicks = 0;
    private float wanderPhase = 0.0F;

    public FireflyEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.lifetime = 800 + level.getRandom().nextInt(600);
        this.wanderPhase = level.getRandom().nextFloat() * 100.0F;
    }

    public FireflyVariant getVariant() {
        return this.variant;
    }

    public void setVariant(FireflyVariant variant) {
        this.variant = variant;
    }

    private void pickNewTarget() {
        if (this.homePos == null) return;

        // Roam around current position, anchored gently around home area (within 16 blocks)
        double anchorX = (this.getX() + this.homePos.getX()) * 0.5D;
        double anchorZ = (this.getZ() + this.homePos.getZ()) * 0.5D;

        for (int i = 0; i < 8; i++) {
            double proposedX = anchorX + (this.random.nextDouble() - 0.5D) * 12.0D;
            double proposedZ = anchorZ + (this.random.nextDouble() - 0.5D) * 12.0D;
            double scanStartY = Math.max(this.getY() + 2.0D, this.homePos.getY() + 2.0D);

            this.mutablePos.set(proposedX, scanStartY, proposedZ);
            boolean foundSurface = false;

            for (int j = 0; j < 8; j++) {
                BlockState state = this.level().getBlockState(this.mutablePos);
                if (!state.isAir()) {
                    foundSurface = true;
                    break;
                }
                this.mutablePos.move(0, -1, 0);
            }

            if (foundSurface) {
                double surfaceY = this.mutablePos.getY() + 1.0D;
                // Target a gentle altitude of 0.8 to 2.8 blocks above surface
                double proposedY = surfaceY + 0.8D + (this.random.nextDouble() * 2.0D);

                this.mutablePos.set(proposedX, proposedY, proposedZ);
                BlockState targetState = this.level().getBlockState(this.mutablePos);

                if (targetState.isAir() && targetState.getFluidState().isEmpty()) {
                    this.targetX = proposedX;
                    this.targetY = proposedY;
                    this.targetZ = proposedZ;

                    // 25% chance to linger/hover gracefully when reaching the destination
                    if (this.random.nextFloat() < 0.25F) {
                        this.hoverTicks = 40 + this.random.nextInt(50);
                    }
                    return;
                }
            }
        }

        // Fallback: stay gently above current position or home
        this.targetX = this.getX() + (this.random.nextDouble() - 0.5D) * 4.0D;
        this.targetY = Math.max(this.getY(), this.homePos.getY() + 1.0D);
        this.targetZ = this.getZ() + (this.random.nextDouble() - 0.5D) * 4.0D;
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
        this.xRotO = this.getXRot();

        if (this.age > this.lifetime) {
            this.clearDynamicLight();
            this.discard();
            return;
        }

        // Despawn safely if no player is nearby
        if (this.age % 40 == 0) {
            if (this.level().getNearestPlayer(this, 64.0D) == null) {
                this.clearDynamicLight();
                this.discard();
                return;
            }
        }

        // Check if currently hovering in place
        if (this.hoverTicks > 0) {
            this.hoverTicks--;
            // Gentle hovering drag and organic sinusoidal bobbing
            this.vx *= 0.85D;
            this.vy *= 0.85D;
            this.vz *= 0.85D;
            this.vy += Math.sin(this.age * 0.12D + this.wanderPhase) * 0.003D;

            if (this.hoverTicks == 0) {
                this.pickNewTarget();
            }
        } else {
            // Travelling towards target with organic curving flight
            double dx = this.targetX - this.getX();
            double dy = this.targetY - this.getY();
            double dz = this.targetZ - this.getZ();
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq < 0.64D || this.age % 120 == 0) {
                this.pickNewTarget();
            } else {
                double dist = Math.sqrt(distSq);
                double cruiseSpeed = 0.035D;
                double accel = 0.06D;

                // Smooth steering towards waypoint
                this.vx += (dx / dist * cruiseSpeed - this.vx) * accel;
                this.vy += (dy / dist * cruiseSpeed - this.vy) * accel;
                this.vz += (dz / dist * cruiseSpeed - this.vz) * accel;

                // Subtle organic harmonic meander for lifelike curved trajectories
                this.vx += Math.cos(this.age * 0.06D + this.wanderPhase) * 0.0025D;
                this.vz += Math.sin(this.age * 0.06D + this.wanderPhase) * 0.0025D;
                this.vy += Math.sin(this.age * 0.10D + this.wanderPhase) * 0.0018D;
            }
        }

        // Obstacle avoidance: steer away from solid collision shapes ahead
        double lookAheadX = this.getX() + this.vx * 12.0D;
        double lookAheadY = this.getY() + this.vy * 12.0D;
        double lookAheadZ = this.getZ() + this.vz * 12.0D;
        this.mutablePos.set(lookAheadX, lookAheadY, lookAheadZ);
        BlockState stateAhead = this.level().getBlockState(this.mutablePos);
        if (!stateAhead.isAir() && !stateAhead.getCollisionShape(this.level(), this.mutablePos).isEmpty()) {
            this.vy += 0.015D;
            this.vx *= 0.7D;
            this.vz *= 0.7D;
        }

        // Water and ground clearance
        if (this.isInWater()) {
            this.vy += 0.04D;
        } else {
            this.mutablePos.set(this.getX(), this.getY() - 0.7D, this.getZ());
            if (!this.level().getBlockState(this.mutablePos).isAir()) {
                this.vy += 0.012D;
            }
        }

        // Apply position
        this.setPos(this.getX() + this.vx, this.getY() + this.vy, this.getZ() + this.vz);

        // Smooth rotation
        double horizontalDistSq = this.vx * this.vx + this.vz * this.vz;
        if (horizontalDistSq > 0.00004D) {
            float targetYaw = (float) (Mth.atan2(this.vz, this.vx) * (180.0D / Math.PI)) + 90.0F;
            this.setYRot(Mth.approachDegrees(this.getYRot(), targetYaw, 8.0F));

            float horizontalSpeed = (float) Math.sqrt(horizontalDistSq);
            float targetPitch = (float) (-Mth.atan2(this.vy, horizontalSpeed) * (180.0D / Math.PI)) * 0.4F;
            targetPitch = Mth.clamp(targetPitch, -30.0F, 30.0F);
            this.setXRot(Mth.approachDegrees(this.getXRot(), targetPitch, 6.0F));
        }

        // Ambient dynamic light (safe client-side update with Block.UPDATE_CLIENTS)
        if (this.age % 2 == 0) {
            this.updateDynamicLight();
        }

        // Ambient glow particle effect
        if (FireflyConfig.get().enableParticles && this.level().isClientSide() && this.random.nextFloat() < 0.05F) {
            double px = this.getX() + (this.random.nextDouble() - 0.5D) * 0.1D;
            double py = this.getY() + (this.random.nextDouble() - 0.5D) * 0.1D + 0.1D;
            double pz = this.getZ() + (this.random.nextDouble() - 0.5D) * 0.1D;

            this.level().addParticle(
                    ParticleTypes.GLOW,
                    px, py, pz,
                    0.0D, 0.0D, 0.0D
            );
        }
    }

    private void updateDynamicLight() {
        if (!this.level().isClientSide() || !FireflyConfig.get().enableDynamicLight) {
            this.clearDynamicLight();
            return;
        }

        BlockPos currentPos = this.blockPosition();
        if (currentPos.equals(this.lastLightPos)) {
            return;
        }

        this.clearDynamicLight();

        if (this.level().getBlockState(currentPos).isAir()) {
            this.level().setBlock(currentPos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 8), Block.UPDATE_CLIENTS);
            this.lastLightPos = currentPos.immutable();
        }
    }

    private void clearDynamicLight() {
        if (this.lastLightPos != null) {
            if (this.level().isClientSide() && this.level().getBlockState(this.lastLightPos).is(Blocks.LIGHT)) {
                this.level().setBlock(this.lastLightPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
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
    public void onClientRemoval() {
        this.clearDynamicLight();
        super.onClientRemoval();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        return false;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected void readAdditionalSaveData(ValueInput input) {}
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
}
