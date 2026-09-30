package com.atom.firefly;

import com.atom.firefly.block.FireflyJarBlock;
import com.atom.firefly.client.FireflyCommand;
import com.atom.firefly.client.FireflyEntity;
import com.atom.firefly.item.FireflyJarItem;
import com.atom.firefly.network.CatchFireflyPayload;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Constants.MOD_ID)
public class FireflyNeoForge {

    public static final ResourceKey<EntityType<?>> FIREFLY_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "firefly")
    );

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Constants.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<FireflyEntity>> FIREFLY_ENTITY = ENTITY_TYPES.register(
            "firefly",
            () -> EntityType.Builder.<FireflyEntity>of(FireflyEntity::new, MobCategory.AMBIENT)
                    .sized(0.35F, 0.35F)
                    .build(FIREFLY_KEY)
    );

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Constants.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIREFLY_PARTICLE = PARTICLE_TYPES.register(
            "firefly",
            () -> new SimpleParticleType(false)
    );

    public static final ResourceKey<Block> FIREFLY_JAR_BLOCK_KEY = ResourceKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "firefly_jar")
    );

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Constants.MOD_ID);

    public static final DeferredHolder<Block, Block> FIREFLY_JAR_BLOCK = BLOCKS.register(
            "firefly_jar",
            () -> new FireflyJarBlock(BlockBehaviour.Properties.of()
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

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Constants.MOD_ID);

    public static final DeferredHolder<Item, Item> FIREFLY_JAR_ITEM = ITEMS.register(
            "firefly_jar",
            () -> new FireflyJarItem(FIREFLY_JAR_BLOCK.get(), new Item.Properties().setId(FIREFLY_JAR_ITEM_KEY).useBlockDescriptionPrefix())
    );

    public FireflyNeoForge(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        PARTICLE_TYPES.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);

        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::addCreativeTab);

        NeoForge.EVENT_BUS.addListener(FireflyCommand::register);

        CommonClass.init();
        Constants.LOG.info("FireFly 3D (NeoForge) initialized!");
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Constants.MOD_ID);
        registrar.playToServer(
                CatchFireflyPayload.TYPE,
                CatchFireflyPayload.CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        InteractionHand hand = payload.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                        ItemStack held = player.getItemInHand(hand);
                        if (held.is(Items.GLASS_BOTTLE)) {
                            if (!player.getAbilities().instabuild) {
                                held.shrink(1);
                            }
                            ItemStack jarStack = new ItemStack(FIREFLY_JAR_ITEM.get());
                            if (!player.getInventory().add(jarStack)) {
                                player.drop(jarStack, false);
                            }
                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
                        }
                    }
                }
        );
    }

    private void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(FIREFLY_JAR_ITEM.get());
        }
    }
}