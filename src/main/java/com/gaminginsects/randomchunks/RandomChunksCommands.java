package com.gaminginsects.randomchunks;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RandomChunksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RandomChunksCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("randomchunks")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("on")
                                .executes(ctx -> {
                                    RandomChunks.ENABLED = true;
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("RandomChunks: ON (chunks will now randomize)"),
                                            true
                                    );
                                    return 1;
                                }))
                        .then(Commands.literal("off")
                                .executes(ctx -> {
                                    RandomChunks.ENABLED = false;
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("RandomChunks: OFF (no more random chunks)"),
                                            true
                                    );
                                    return 1;
                                }))
        );

        dispatcher.register(
                Commands.literal("randomchunksRare")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("on")
                                .executes(ctx -> {
                                    RandomChunks.RARE_BOOST = true;
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("RandomChunks rare blocks: ON"),
                                            true
                                    );
                                    return 1;
                                }))
                        .then(Commands.literal("off")
                                .executes(ctx -> {
                                    RandomChunks.RARE_BOOST = false;
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("RandomChunks rare blocks: OFF"),
                                            true
                                    );
                                    return 1;
                                }))
        );
    }
}
