package knightminer.inspirations.shared;

import knightminer.inspirations.Inspirations;
import knightminer.inspirations.common.ClientProxy;
import knightminer.inspirations.library.Util;
import knightminer.inspirations.library.client.ClientUtil;
import knightminer.inspirations.shared.client.TextureModel;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.concurrent.CompletableFuture;

public class SharedClientProxy extends ClientProxy {
	@SubscribeEvent
	public void setup(FMLCommonSetupEvent event) {
		// listener to clear color cache from client utils
		IResourceManager manager = Minecraft.getInstance().getResourceManager();
		// should always be true, but just in case
		if(manager instanceof IReloadableResourceManager) {
			((IReloadableResourceManager) manager).addReloadListener(
					(stage, resMan, prepProp, reloadProf, bgExec, gameExec) -> CompletableFuture
									.runAsync(ClientUtil::clearCache, gameExec)
									.thenCompose(stage::markCompleteAwaitingOthers)
			);
		} else {
			Inspirations.log.error("Failed to register resource reload listener, expected instance of IReloadableResourceManager but got {}", manager.getClass());
		}
		MinecraftForge.EVENT_BUS.addListener(TextureModel::onTextureStitchPre);
		MinecraftForge.EVENT_BUS.addListener(TextureModel::onTextureStitchPost);
	}

	@SubscribeEvent
	public void registerTextures(TextureStitchEvent.Pre event) {
		// ensures the colorless fluid texture is loaded.
		if (event.getMap().getBasePath().equals("texture")) {
			event.addSprite(Util.getResource("blocks/fluid_colorless"));
			event.addSprite(Util.getResource("blocks/fluid_colorless_flow"));
		}
	}
}
