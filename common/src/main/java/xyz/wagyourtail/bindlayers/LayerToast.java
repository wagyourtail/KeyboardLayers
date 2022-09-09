package xyz.wagyourtail.bindlayers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LayerToast extends SystemToast {
    boolean isDone = false;

    public LayerToast(SystemToastIds systemToastIds, Component component, @Nullable Component component2) {
        super(systemToastIds, component, component2);
    }


    @Override
    public Visibility render(@NotNull PoseStack poseStack, @NotNull ToastComponent toastComponent, long l) {
        Visibility sup = super.render(poseStack, toastComponent, l);
        if (sup == Visibility.HIDE) {
            isDone = true;
        } else {
            isDone = false;
        }
        return sup;
    }

}
