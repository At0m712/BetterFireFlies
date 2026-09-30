package com.atom.firefly.client;

import com.atom.firefly.config.FireflyConfig;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

public class FireflyCommand {

    public static void register(RegisterClientCommandsEvent event) {
        // Dynamic light toggle command
        event.getDispatcher().register(Commands.literal("fireflylight")
                .executes(context -> {
                    FireflyConfig config = FireflyConfig.get();
                    config.enableDynamicLight = !config.enableDynamicLight;
                    FireflyConfig.save();

                    String status = config.enableDynamicLight ? "§aENABLED" : "§cDISABLED";
                    context.getSource().sendSystemMessage(
                            Component.literal("§e[FireFly] §fDynamic light : " + status)
                    );
                    return 1;
                })
        );

        // Ambient particles toggle command
        event.getDispatcher().register(Commands.literal("fireflyparticles")
                .executes(context -> {
                    FireflyConfig config = FireflyConfig.get();
                    config.enableParticles = !config.enableParticles;
                    FireflyConfig.save();

                    String status = config.enableParticles ? "§aENABLED" : "§cDISABLED";
                    context.getSource().sendSystemMessage(
                            Component.literal("§e[FireFly] §fParticles : " + status)
                    );
                    return 1;
                })
        );
    }
}