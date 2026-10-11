package com.atom.firefly.block;

import com.atom.firefly.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FireflyJarBlock extends LanternBlock {

    protected static final VoxelShape STANDING_SHAPE = Shapes.or(
            box(4.0D, 0.0D, 4.0D, 12.0D, 10.0D, 12.0D),
            box(5.0D, 10.0D, 5.0D, 11.0D, 12.0D, 11.0D)
    );

    protected static final VoxelShape HANGING_SHAPE = Shapes.or(
            box(4.0D, 2.0D, 4.0D, 12.0D, 12.0D, 12.0D),
            box(5.0D, 12.0D, 5.0D, 11.0D, 14.0D, 11.0D),
            box(6.5D, 14.0D, 6.5D, 9.5D, 16.0D, 9.5D)
    );

    public FireflyJarBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HANGING) ? HANGING_SHAPE : STANDING_SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double yCenter = state.getValue(HANGING) ? 0.48D : 0.32D;

        ParticleOptions particleToSpawn = ParticleTypes.GLOW;
        ParticleType<?> customType = BuiltInRegistries.PARTICLE_TYPE.getOptional(
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "firefly")
        ).orElse(null);

        if (customType instanceof SimpleParticleType simpleParticle) {
            particleToSpawn = simpleParticle;
        }

        // Spawn 2 to 4 animated 2D firefly particles inside the glass jar
        int count = 2 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            double px = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.24D;
            double py = pos.getY() + yCenter + (random.nextDouble() - 0.5D) * 0.16D;
            double pz = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.24D;
            level.addAlwaysVisibleParticle(particleToSpawn, true, px, py, pz, 0.0D, 0.0D, 0.0D);
        }
    }
}
