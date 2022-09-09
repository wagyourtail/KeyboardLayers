package xyz.wagyourtail.bindlayers.forge;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
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
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(
                (mc, parent) -> new LayerManagementScreen(parent)
            )
        );
    }

    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent event) {
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent tick) {
        if (tick.phase == TickEvent.Phase.END) {
            BindLayers.INSTANCE.onTick();
        }
    }

    @SubscribeEvent
    public void onKeyBinds(RegisterKeyMappingsEvent event) {
        event.register(BindLayers.INSTANCE.nextLayer);
        event.register(BindLayers.INSTANCE.prevLayer);
        event.register(BindLayers.INSTANCE.quickSelect);
    }

}
