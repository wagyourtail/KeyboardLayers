package xyz.wagyourtail.bindlayers.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
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
    @SuppressWarnings("ConstantConditions")
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
        //        System.out.println("saving layers");
        Set<String> layers = BindLayers.INSTANCE.availableLayers();
        for (String layer : layers) {
            BindLayer l = BindLayers.INSTANCE.getOrCreate(layer);
            if (l != BindLayers.INSTANCE.vanillaLayer) {
                //                System.out.println("saving layer: " + layer);
                l.save();
            }
        }

        BindLayers.INSTANCE.vanillaLayer.applyLayer();

    }

    @Inject(method = "setKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;save()V"))
    public void bindlayers$onKeySet(KeyMapping m, InputConstants.Key k, CallbackInfo ci) {
        BindLayer layer = BindLayers.INSTANCE.getOrCreate(BindLayers.INSTANCE.getActiveLayer());
        layer.binds.put(m, BindLayers.provider.keyMappingToBind(m));
    }

    @Inject(method = "save", at = @At("RETURN"))
    public void bindlayers$onSaveReturn(CallbackInfo ci) {
        // reload layers
        BindLayers.INSTANCE.setActiveLayer(BindLayers.INSTANCE.getActiveLayer(), true);
    }

}
