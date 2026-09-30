package com.atom.firefly.client;

import com.atom.firefly.Constants;
import com.atom.firefly.FireflyNeoForge;
import com.atom.firefly.network.CatchFireflyPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.neoforged.neoforge.network.registration.NetworkPayloadSetup;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class FireflyNeoForgeClient {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FireflyNeoForge.FIREFLY_ENTITY.get(), FireflyRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FireflyModel.LAYER_LOCATION, FireflyModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(FireflyNeoForge.FIREFLY_PARTICLE.get(), FireflyJarParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && !client.isPaused()) {
            FireflySpawner.trySpawn(client, FireflyNeoForge.FIREFLY_ENTITY.get());
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Level world = event.getLevel();
        if (world.isClientSide() && event.getTarget() instanceof FireflyEntity firefly) {
            Player player = event.getEntity();
            InteractionHand hand = event.getHand();
            ItemStack held = player.getItemInHand(hand);
            if (held.is(Items.GLASS_BOTTLE)) {
                boolean canSend = canSendCatchPayload();
                if (canSend) {
                    player.playSound(SoundEvents.BOTTLE_FILL, 1.0F, 1.0F);
                    for (int i = 0; i < 8; i++) {
                        double px = firefly.getX() + (world.getRandom().nextDouble() - 0.5D) * 0.3D;
                        double py = firefly.getY() + (world.getRandom().nextDouble() - 0.5D) * 0.3D;
                        double pz = firefly.getZ() + (world.getRandom().nextDouble() - 0.5D) * 0.3D;
                        world.addParticle(ParticleTypes.GLOW, px, py, pz, 0.0D, 0.0D, 0.0D);
                    }
                    firefly.discard();
                    ClientPacketDistributor.sendToServer(new CatchFireflyPayload(hand == InteractionHand.MAIN_HAND));
                    event.setCancellationResult(InteractionResult.SUCCESS);
                } else {
                    player.sendOverlayMessage(
                            Component.translatable("chat.firefly.server_required")
                    );
                    event.setCancellationResult(InteractionResult.CONSUME);
                }
                event.setCanceled(true);
            }
        }
    }

    private static boolean canSendCatchPayload() {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        if (listener == null) return false;
        Connection connection = listener.getConnection();
        ConnectionType type = ChannelAttributes.getConnectionType(connection);
        if (type != null && type.isNeoForge()) {
            NetworkPayloadSetup setup = ChannelAttributes.getPayloadSetup(connection);
            return setup != null && setup.getChannel(ConnectionProtocol.PLAY, CatchFireflyPayload.TYPE.id()) != null;
        }
        return false;
    }
}