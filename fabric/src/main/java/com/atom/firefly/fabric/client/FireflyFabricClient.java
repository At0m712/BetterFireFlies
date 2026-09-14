package com.atom.firefly.fabric.client;

import com.atom.firefly.client.FireflyEntity;
import com.atom.firefly.client.FireflyModel;
import com.atom.firefly.client.FireflyRenderer;
import com.atom.firefly.client.FireflySpawner;
import com.atom.firefly.fabric.FireflyFabric;
import com.atom.firefly.fabric.network.CatchFireflyPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FireflyFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 1. Enregistrement du modele de la luciole
        EntityModelLayerRegistry.registerModelLayer(FireflyModel.LAYER_LOCATION, FireflyModel::createBodyLayer);

        // 2. Rendu de l'entite luciole
        EntityRendererRegistry.register(FireflyFabric.FIREFLY_ENTITY, FireflyRenderer::new);

        // 3. Apparition naturelle des lucioles
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            FireflySpawner.trySpawn(client, FireflyFabric.FIREFLY_ENTITY);
        });

        // 4. Enregistrement de la particule 2D de luciole dans le bocal
        ParticleFactoryRegistry.getInstance().register(FireflyFabric.FIREFLY_PARTICLE, FireflyJarParticle.Provider::new);

        // 5. Couche de rendu translucide pour le verre du bocal
        BlockRenderLayerMap.INSTANCE.putBlock(FireflyFabric.FIREFLY_JAR_BLOCK, RenderType.translucent());

        // 6. Capture de la luciole avec fiole (Support hybride Graceful Fallback)
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide() && entity instanceof FireflyEntity firefly) {
                ItemStack held = player.getItemInHand(hand);
                if (held.is(Items.GLASS_BOTTLE)) {
                    if (ClientPlayNetworking.canSend(CatchFireflyPayload.TYPE)) {
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
                    } else {
                        // Serveur Vanilla / sans le mod : repli propre sans crash
                        player.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable("chat.firefly.server_required"),
                                true
                        );
                        return InteractionResult.CONSUME;
                    }
                }
            }
            return InteractionResult.PASS;
        });

        // 7. Commandes /fireflylight et /fireflyparticles
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            FireflyCommand.register(dispatcher);
        });
    }
}