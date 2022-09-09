package xyz.wagyourtail.bindlayers;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

public class BindLayersFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(BindLayers.INSTANCE.nextLayer);
        KeyBindingHelper.registerKeyBinding(BindLayers.INSTANCE.prevLayer);
        KeyBindingHelper.registerKeyBinding(BindLayers.INSTANCE.quickSelect);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> BindLayers.INSTANCE.onTick());
    }

}
