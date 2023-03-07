package xyz.wagyourtail.bindlayers.mixin.compat.controlling;

import com.blamejared.controlling.client.NewKeyBindsList;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.wagyourtail.bindlayers.BindLayer;
import xyz.wagyourtail.bindlayers.BindLayers;

import java.util.function.Supplier;

import static net.minecraft.network.chat.Component.literal;

@Pseudo
@Mixin(NewKeyBindsList.KeyEntry.class)
public class MixinKeyEntry {

    @Shadow
    @Final
    private KeyMapping keybinding;


    @Inject(method = {"lambda$new$1"}, at = @At(value = "RETURN"))
    private static void bindlayers$onKeyReset(KeyMapping name, Component keyDesc, Supplier supp, CallbackInfoReturnable<MutableComponent> cir) {
        BindLayer layer = BindLayers.INSTANCE.getOrCreate(BindLayers.INSTANCE.getActiveLayer());
        if (layer == BindLayers.INSTANCE.vanillaLayer) {
            return;
        }
        layer.binds.remove(name);
    }

    @Redirect(method = {"render", "method_25343", "m_6311_"},
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;draw(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/Component;FFI)I"))
    private int bindlayers$onRenderName(Font instance, PoseStack poseStack, Component text, float x, float y, int color) {
        if (!BindLayers.INSTANCE.getOrCreate(BindLayers.INSTANCE.getActiveLayer()).binds.containsKey(keybinding)) {
            return instance.draw(
                poseStack,
                text.copy().withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY),
                x,
                y,
                color
            );
        }
        return instance.draw(poseStack, text, x, y, color);
    }

}
