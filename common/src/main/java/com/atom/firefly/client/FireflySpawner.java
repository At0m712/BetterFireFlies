package com.atom.firefly.client;

import com.atom.firefly.config.FireflyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.AABB;

public class FireflySpawner {

    private static int clientEntityIdCounter = -10000;

    public static void trySpawn(Minecraft client, EntityType<FireflyEntity> entityType) {
        ClientLevel level = client.level;
        if (level == null || client.player == null || client.isPaused()) return;

        FireflyConfig config = FireflyConfig.get();
        if (level.getRandom().nextInt(Math.max(1, config.spawnChance)) != 0) return;

        // Dynamic leak-free entity counting around the player
        AABB searchBox = client.player.getBoundingBox().inflate(64.0D);
        int currentCount = level.getEntitiesOfClass(FireflyEntity.class, searchBox).size();
        if (currentCount >= config.maxFireflies) return;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(
                client.player.getX() + (level.getRandom().nextDouble() - 0.5) * 64,
                client.player.getY() + 6,
                client.player.getZ() + (level.getRandom().nextDouble() - 0.5) * 64
        );

        // Find ground level below proposed spawn pos
        boolean foundGround = false;
        for (int i = 0; i < 24; i++) {
            if (!level.getBlockState(pos).isAir()) {
                foundGround = true;
                break;
            }
            pos.move(0, -1, 0);
        }

        if (!foundGround) return;
        pos.move(0, 1, 0);

        boolean isDarkCave = level.getBrightness(LightLayer.SKY, pos) == 0 && level.getBrightness(LightLayer.BLOCK, pos) < 4;
        long timeOfDay = level.getOverworldClockTime() % 24000;
        boolean isNightOrStorm = (timeOfDay >= 13000 && timeOfDay < 23000) || level.isThundering();

        // Caves can spawn anytime; surface biomes spawn only at night or during dark storms
        if (!isDarkCave && !isNightOrStorm) return;

        boolean isAllowed = isDarkCave;
        if (!isAllowed) {
            Holder<Biome> biome = level.getBiome(pos);
            isAllowed = biome.is(BiomeTags.IS_FOREST)
                    || biome.is(BiomeTags.IS_JUNGLE)
                    || biome.is(Biomes.SWAMP)
                    || biome.is(Biomes.MANGROVE_SWAMP)
                    || biome.is(Biomes.LUSH_CAVES)
                    || biome.is(Biomes.CHERRY_GROVE);
        }

        if (!isAllowed) return;

        int remaining = config.maxFireflies - currentCount;
        int clusterSize = Math.min(remaining, 2 + level.getRandom().nextInt(4));
        int baseX = pos.getX();
        int baseY = pos.getY();
        int baseZ = pos.getZ();

        for (int i = 0; i < clusterSize; i++) {
            double finalX = baseX + (level.getRandom().nextDouble() - 0.5) * 6.0;
            double finalY = baseY + 0.5 + level.getRandom().nextDouble() * 2.0;
            double finalZ = baseZ + (level.getRandom().nextDouble() - 0.5) * 6.0;

            pos.set(finalX, finalY, finalZ);

            if (level.getBlockState(pos).isAir()) {
                FireflyEntity firefly = new FireflyEntity(entityType, level);
                firefly.setPos(finalX, finalY, finalZ);
                firefly.setId(clientEntityIdCounter--);
                firefly.setVariant(FireflyVariant.fromBiome(level.getBiome(pos)));
                level.addEntity(firefly);
            }
        }
    }
}