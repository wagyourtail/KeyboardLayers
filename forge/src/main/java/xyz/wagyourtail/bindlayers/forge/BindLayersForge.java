package xyz.wagyourtail.bindlayers.forge;

import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import xyz.wagyourtail.bindlayers.BindLayers;
import xyz.wagyourtail.bindlayers.screen.LayerManagementScreen;

@Mod("bindlayers")
public class BindLayersForge {

    public BindLayersForge() {
        ModLoadingContext.get().registerExtensionPoint(
            ConfigGuiHandler.ConfigGuiFactory.class,
            () -> new ConfigGuiHandler.ConfigGuiFactory(
                (mc, parent) -> new LayerManagementScreen(parent)
            )
        );
    }

    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent event) {
        onKeyBinds();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent tick) {
        if (tick.phase == TickEvent.Phase.END) {
            BindLayers.INSTANCE.onTick();
        }
    }

    public void onKeyBinds() {
        ClientRegistry.registerKeyBinding(BindLayers.INSTANCE.nextLayer);
        ClientRegistry.registerKeyBinding(BindLayers.INSTANCE.prevLayer);
        ClientRegistry.registerKeyBinding(BindLayers.INSTANCE.quickSelect);
    }

}
