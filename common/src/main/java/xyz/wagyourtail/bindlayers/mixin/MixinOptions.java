package xyz.wagyourtail.bindlayers.mixin;

import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.wagyourtail.bindlayers.BindLayer;
import xyz.wagyourtail.bindlayers.BindLayers;

import java.io.IOException;
import java.util.Set;

@Mixin(Options.class)
public class MixinOptions {

    @Inject(method = "load", at = @At("RETURN"))
    public void onLoad(CallbackInfo ci) {
        try {
            BindLayers.INSTANCE.onGameOptionsLoad((Options) (Object) this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Inject(method = "save", at = @At("HEAD"))
    public void bindlayers$onSave(CallbackInfo ci) throws IOException {
        // save layers
        System.out.println("saving layers");
        Set<String> layers = BindLayers.INSTANCE.availableLayers();
        for (String layer : layers) {
            BindLayer l = BindLayers.INSTANCE.getOrCreate(layer);
            if (l != BindLayers.INSTANCE.defaultLayer) {
                System.out.println("saving layer: " + layer);
                l.save();
            }
        }

        BindLayers.INSTANCE.defaultLayer.applyLayer();

    }

    @Inject(method = "save", at = @At("RETURN"))
    public void bindlayers$onSaveReturn(CallbackInfo ci) {
        // reload layers
        BindLayers.INSTANCE.setActiveLayer(BindLayers.INSTANCE.getActiveLayer());
    }

}
