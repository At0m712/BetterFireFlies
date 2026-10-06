package com.atom.firefly.fabric.client;

import com.atom.firefly.config.FireflyConfig;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public class FireflyCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {

        // Dynamic light toggle command
        dispatcher.register(ClientCommands.literal("fireflylight")
                .executes(context -> {
                    FireflyConfig config = FireflyConfig.get();
                    config.enableDynamicLight = !config.enableDynamicLight;
                    FireflyConfig.save();

                    String key = config.enableDynamicLight ? "command.firefly.light.enabled" : "command.firefly.light.disabled";
                    context.getSource().sendFeedback(
                            Component.literal("§e[FireFly] §f").append(Component.translatable(key))
                    );
                    return 1;
                })
        );

        // Ambient particles toggle command
        dispatcher.register(ClientCommands.literal("fireflyparticles")
                .executes(context -> {
                    FireflyConfig config = FireflyConfig.get();
                    config.enableParticles = !config.enableParticles;
                    FireflyConfig.save();

                    String key = config.enableParticles ? "command.firefly.particles.enabled" : "command.firefly.particles.disabled";
                    context.getSource().sendFeedback(
                            Component.literal("§e[FireFly] §f").append(Component.translatable(key))
                    );
                    return 1;
                })
        );
    }
}