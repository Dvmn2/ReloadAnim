package net.dvmn2.reloadanim.client;

import net.dvmn2.reloadanim.client.animation.AnimationLibrary;
import net.dvmn2.reloadanim.client.animation.PackAnimationReloader;
import net.dvmn2.reloadanim.client.command.ReloadAnimCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

/**
 * Client entry point: registers the pack reloader and the client command, nothing else.
 *
 * <p>Runs on the client thread during game start-up.
 */
public final class ReloadAnimClient implements ClientModInitializer {

    /** Mod identifier; also the namespace of every resource and the name of the logger. */
    public static final String MOD_ID = "reloadanim";

    private static final String RELOADER_PATH = "pack_animation";

    @Override
    public void onInitializeClient() {
        // The first loading overlay appears before any reload finishes, so the cache is read right away.
        AnimationLibrary.refreshAsync();

        ResourceLoader.get(ResourceType.CLIENT_RESOURCES)
                .registerReloader(Identifier.of(MOD_ID, RELOADER_PATH), new PackAnimationReloader());

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ReloadAnimCommand.build()));
    }
}
