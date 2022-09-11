package xyz.wagyourtail.bindlayers.fabric.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.controls.KeyBindsList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static xyz.wagyourtail.bindlayers.AmecAccessor.resetAmecsKeybind;

@Mixin(KeyBindsList.KeyEntry.class)
public class MixinKeyEntry {
    @Inject(method = "method_19870", at = @At(value = "HEAD"))
    public void bindlayers$onKeyReset(KeyMapping keyMapping, Button button, CallbackInfo ci) {
        if (FabricLoader.getInstance().isModLoaded("amecsapi")) {
            resetAmecsKeybind(keyMapping);
        }
    }

}
