package xyz.wagyourtail.bindlayers;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.commons.lang3.ArrayUtils;

@Mod("bindlayers")
public class BindLayersForge extends BindLayers {

    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent event) {
    }

}
