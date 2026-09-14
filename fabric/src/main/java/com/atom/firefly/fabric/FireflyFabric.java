package com.atom.firefly.fabric;

import com.atom.firefly.CommonClass;
import com.atom.firefly.Constants;
import com.atom.firefly.block.FireflyJarBlock;
import com.atom.firefly.client.FireflyEntity;
import com.atom.firefly.fabric.network.CatchFireflyPayload;
import com.atom.firefly.item.FireflyJarItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class FireflyFabric implements ModInitializer {

    // 1. Clé et type d'entité luciole
    public static final ResourceKey<EntityType<?>> FIREFLY_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "firefly")
    );

    public static final EntityType<FireflyEntity> FIREFLY = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "firefly"),
            EntityType.Builder.<FireflyEntity>of(FireflyEntity::new, MobCategory.AMBIENT)
                    .sized(0.35F, 0.35F)
                    .build(FIREFLY_KEY)
    );

    // 2. Particule 2D de luciole
    public static final SimpleParticleType FIREFLY_PARTICLE = Registry.register(
            BuiltInRegistries.PARTICLE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "firefly"),
            net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple()
    );

    // 3. Clé et enregistrement du Bocal de luciole
    public static final ResourceKey<Block> FIREFLY_JAR_BLOCK_KEY = ResourceKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "firefly_jar")
    );

    public static final Block FIREFLY_JAR_BLOCK = Registry.register(
            BuiltInRegistries.BLOCK,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "firefly_jar"),
            new FireflyJarBlock(BlockBehaviour.Properties.of()
                    .setId(FIREFLY_JAR_BLOCK_KEY)
                    .lightLevel(state -> 12)
                    .noOcclusion()
                    .sound(SoundType.GLASS)
                    .strength(0.3F))
    );

    public static final ResourceKey<Item> FIREFLY_JAR_ITEM_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "firefly_jar")
    );

    public static final Item FIREFLY_JAR_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "firefly_jar"),
            new FireflyJarItem(FIREFLY_JAR_BLOCK, new Item.Properties().setId(FIREFLY_JAR_ITEM_KEY).useBlockDescriptionPrefix())
    );

    @Override
    public void onInitialize() {
        CommonClass.init();

        // Réseau : enregistrement du paquet pour capturer une luciole
        PayloadTypeRegistry.serverboundPlay().register(CatchFireflyPayload.TYPE, CatchFireflyPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CatchFireflyPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            InteractionHand hand = payload.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack held = player.getItemInHand(hand);
            if (held.is(Items.GLASS_BOTTLE)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                ItemStack jarStack = new ItemStack(FIREFLY_JAR_ITEM);
                if (!player.getInventory().add(jarStack)) {
                    player.drop(jarStack, false);
                }
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        });

        // Ajout à l'onglet inventaire créatif des blocs fonctionnels
        ResourceKey<net.minecraft.world.item.CreativeModeTab> functionalBlocks = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath("minecraft", "functional_blocks")
        );
        CreativeModeTabEvents.modifyOutputEvent(functionalBlocks).register(output -> {
            output.accept(FIREFLY_JAR_ITEM);
        });

        Constants.LOG.info("FireFly 3D (Fabric) initialized!");
    }
}