package xyz.wagyourtail.bindlayers.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
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
import xyz.wagyourtail.bindlayers.accessor.KeyBindsScreenAccessor;

@Mixin(KeyBindsList.KeyEntry.class)
public class MixinKeyEntry {
    @Shadow @Final private Button resetButton;

    @Shadow @Final KeyBindsList field_2742;

//    @Inject(method = "<init>", at = @At("RETURN"))
//    public void bindlayers$onInit(KeyBindsList keyBindsList, KeyMapping keyMapping, Component component, CallbackInfo ci) {
//        // change reset to be more compact
//        resetButton.setMessage(Component.literal("\u21BB"));
//        resetButton.setWidth(20);
//
//    }

    @Shadow @Final private KeyMapping key;

    @Inject(method = "method_19870", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;resetMapping()V"))
    public void bindlayers$onKeyReset(KeyMapping keyMapping, Button button, CallbackInfo ci) {
        BindLayer l = BindLayers.INSTANCE.getOrCreate(((KeyBindsScreenAccessor) ((MixinKeyBindsList) field_2742).getKeyBindsScreen()).bindlayers$getCurrentLayer());
        if (l == BindLayers.INSTANCE.defaultLayer) {
            l.binds.put(key, key.getDefaultKey());
        } else {
            l.binds.remove(keyMapping);
        }
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;draw(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/Component;FFI)I"))
    private Component bindlayers$onRenderName(Component name) {
        if (!BindLayers.INSTANCE.getOrCreate(((KeyBindsScreenAccessor) ((MixinKeyBindsList) field_2742).getKeyBindsScreen()).bindlayers$getCurrentLayer()).binds.containsKey(key)) {
            return name.copy().withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
        }
        return name;
    }
}
