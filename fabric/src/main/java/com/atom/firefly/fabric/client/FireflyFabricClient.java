package com.atom.firefly.fabric.client;

import com.atom.firefly.client.FireflyEntity;
import com.atom.firefly.client.FireflyModel;
import com.atom.firefly.client.FireflyRenderer;
import com.atom.firefly.client.FireflySpawner;
import com.atom.firefly.fabric.FireflyFabric;
import com.atom.firefly.fabric.network.CatchFireflyPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FireflyFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 1. Enregistrement des modèles et du rendu
        EntityModelLayerRegistry.registerModelLayer(FireflyModel.LAYER_LOCATION, FireflyModel::createBodyLayer);
        EntityRendererRegistry.register(FireflyFabric.FIREFLY, FireflyRenderer::new);

        // 2. Enregistrement de la particule 2D de luciole
        ParticleFactoryRegistry.getInstance().register(FireflyFabric.FIREFLY_PARTICLE, FireflyJarParticle.Provider::new);

        // 3. Enregistrement du spawner de lucioles
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null && !client.isPaused()) {
                FireflySpawner.trySpawn(client, FireflyFabric.FIREFLY);
            }
        });

        // 4. Capture des lucioles avec une fiole en verre
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide() && entity instanceof FireflyEntity firefly) {
                ItemStack held = player.getItemInHand(hand);
                if (held.is(Items.GLASS_BOTTLE)) {
                    player.playSound(SoundEvents.BOTTLE_FILL, 1.0F, 1.0F);
                    for (int i = 0; i < 8; i++) {
                        double px = firefly.getX() + (world.random.nextDouble() - 0.5D) * 0.3D;
                        double py = firefly.getY() + (world.random.nextDouble() - 0.5D) * 0.3D;
                        double pz = firefly.getZ() + (world.random.nextDouble() - 0.5D) * 0.3D;
                        world.addParticle(ParticleTypes.GLOW, px, py, pz, 0.0D, 0.0D, 0.0D);
                    }
                    firefly.discard();
                    ClientPlayNetworking.send(new CatchFireflyPayload(hand == InteractionHand.MAIN_HAND));
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        });

        // 5. Enregistrement des commandes (Lumière dynamique ET Particules)
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            FireflyCommand.register(dispatcher);
        });

        // 6. Couche de rendu translucide pour le bocal en verre
        net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlock(
                FireflyFabric.FIREFLY_JAR_BLOCK,
                net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT
        );
    }
}