package net.dvmn2.reloadanim.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.dvmn2.reloadanim.client.ReloadAnimConfig;
import net.dvmn2.reloadanim.client.animation.AnimationInfo;
import net.dvmn2.reloadanim.client.animation.AnimationLibrary;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

/**
 * Client command tree. {@code off} is the emergency switch: it takes effect on the next frame.
 *
 * <pre>
 * /ra
 * ├── off      disable all mod effects
 * ├── on       enable the mod again
 * ├── status   show the current state
 * └── reload   re-read config/reloadanim.json
 * </pre>
 * Runs on the client thread.
 */
public final class ReloadAnimCommand {

    private static final String ROOT = "ra";

    private ReloadAnimCommand() {
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return ClientCommandManager.literal(ROOT)
                .then(ClientCommandManager.literal("off").executes(ReloadAnimCommand::off))
                .then(ClientCommandManager.literal("on").executes(ReloadAnimCommand::on))
                .then(ClientCommandManager.literal("status").executes(ReloadAnimCommand::status))
                .then(ClientCommandManager.literal("reload").executes(ReloadAnimCommand::reload));
    }

    private static int off(CommandContext<FabricClientCommandSource> ctx) {
        ReloadAnimConfig.get().enabled = false;
        ReloadAnimConfig.save();
        ctx.getSource().sendFeedback(Text.translatable("reloadanim.off"));
        return Command.SINGLE_SUCCESS;
    }

    private static int on(CommandContext<FabricClientCommandSource> ctx) {
        ReloadAnimConfig.get().enabled = true;
        ReloadAnimConfig.save();
        AnimationLibrary.resetFailure();
        AnimationLibrary.refreshAsync();
        ctx.getSource().sendFeedback(Text.translatable("reloadanim.on"));
        return Command.SINGLE_SUCCESS;
    }

    private static int reload(CommandContext<FabricClientCommandSource> ctx) {
        ReloadAnimConfig.reload();
        AnimationLibrary.resetFailure();
        AnimationLibrary.refreshAsync();
        ctx.getSource().sendFeedback(Text.translatable("reloadanim.reloaded"));
        return Command.SINGLE_SUCCESS;
    }

    private static int status(CommandContext<FabricClientCommandSource> ctx) {
        ReloadAnimConfig config = ReloadAnimConfig.get();
        AnimationInfo info = AnimationLibrary.info();
        Text animation = info == null
                ? Text.translatable("reloadanim.animation.none")
                : Text.translatable("reloadanim.animation.cached", info.packId(), info.frames());
        ctx.getSource().sendFeedback(Text.translatable("reloadanim.status",
                state(config.enabled),
                state(config.allowPackAnimations),
                animation,
                String.valueOf(config.intensity),
                state(config.reducedMotion)));
        return Command.SINGLE_SUCCESS;
    }

    private static Text state(boolean value) {
        return Text.translatable(value ? "reloadanim.state.on" : "reloadanim.state.off");
    }
}
