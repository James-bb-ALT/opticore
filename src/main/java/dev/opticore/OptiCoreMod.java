package dev.opticore;

import com.mojang.brigadier.Command;
import dev.opticore.config.OptiCoreConfig;
import dev.opticore.core.OptiCoreMetrics;
import dev.opticore.core.StringInternPool;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OptiCore entrypoint: registers config, diagnostic commands, and lifecycle telemetry.
 */
@Mod(OptiCoreMod.MOD_ID)
public final class OptiCoreMod {
    public static final String MOD_ID = "opticore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public OptiCoreMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, OptiCoreConfig.COMMON_SPEC);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
        LOGGER.info("[OptiCore] Initialized targeted CPU/RAM/Thread optimization subsystems.");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("opticore")
                        .then(Commands.literal("stats").executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal(OptiCoreMetrics.formatSummary()), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(Commands.literal("trim").requires(src -> src.hasPermission(2)).executes(ctx -> {
                            StringInternPool.trimIfPressured();
                            OptiCoreMetrics.MEMORY_TRIMS.incrementAndGet();
                            ctx.getSource().sendSuccess(() -> Component.literal("[OptiCore] Trimmed internal caches."), false);
                            return Command.SINGLE_SUCCESS;
                        }))
        );
    }

    private void onServerStarted(ServerStartedEvent event) {
        if (OptiCoreConfig.ENABLE_THREAD_PRIORITY_HYGIENE.get()) {
            Thread serverThread = event.getServer().getRunningThread();
            if (serverThread != null && serverThread.getPriority() < Thread.NORM_PRIORITY + 2) {
                serverThread.setPriority(Math.min(Thread.MAX_PRIORITY, Thread.NORM_PRIORITY + 3));
            }
        }
    }

    private void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info(OptiCoreMetrics.formatSummary());
    }
}
