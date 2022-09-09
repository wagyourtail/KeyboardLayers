package xyz.wagyourtail.bindlayers.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import xyz.wagyourtail.bindlayers.BindLayer;
import xyz.wagyourtail.bindlayers.BindLayers;

public class RenameLayerScreen extends Screen {
    public final Screen parent;
    public final String oldName;
    public String newName;

    public RenameLayerScreen(Screen parent, String layerName) {
        super(Component.literal("Rename Layer"));
        this.parent = parent;
        this.oldName = layerName;
        this.newName = layerName;
    }



    @Override
    protected void init() {
        super.init();

        EditBox nameBox = addRenderableWidget(
            new EditBox(font, width / 2 - 100, height / 2 - 10, 200, 20, Component.literal("New Name"))
        );
        nameBox.setValue(newName);
        nameBox.setResponder(s -> newName = s);

    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
        if (newName.equals(oldName)) return;
        BindLayer newLayer = BindLayers.INSTANCE.getOrCreate(newName);
        newLayer.copyFrom(BindLayers.INSTANCE.getOrCreate(oldName));
        if (BindLayers.INSTANCE.getActiveLayer().equals(oldName)) {
            BindLayers.INSTANCE.setActiveLayer(newName);
        }
        BindLayer oldLayer = BindLayers.INSTANCE.removeLayer(oldName);
        oldLayer.removeFile();
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);

        drawCenteredString(poseStack, font, title, width / 2, 10, 0xFFFFFF);

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

}
