package xyz.wagyourtail.bindlayers.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.wagyourtail.bindlayers.BindLayer;
import xyz.wagyourtail.bindlayers.BindLayers;

@Mixin(KeyBindsList.KeyEntry.class)
public class MixinKeyEntry {
    @Shadow(aliases = "keybinding")
    @Final
    private KeyMapping key;


    @Inject(method = { "method_19870", "m_193933_" }, at = @At(value = "RETURN"))
    public void bindlayers$onKeyReset(KeyMapping keyMapping, Button button, CallbackInfo ci) {
        BindLayer layer = BindLayers.INSTANCE.getOrCreate(BindLayers.INSTANCE.getActiveLayer());
        if (layer == BindLayers.INSTANCE.defaultLayer) {
            return;
        }
        layer.binds.remove(keyMapping);
    }

    @ModifyArg(method = "render",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;draw(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/Component;FFI)I"))
    private Component bindlayers$onRenderName(Component name) {
        if (!BindLayers.INSTANCE.getOrCreate(BindLayers.INSTANCE.getActiveLayer()).binds.containsKey(key)) {
            return name.copy().withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
        }
        return name;
    }

}
