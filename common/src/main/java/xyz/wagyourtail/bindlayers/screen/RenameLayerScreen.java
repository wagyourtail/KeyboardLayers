package xyz.wagyourtail.bindlayers.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import xyz.wagyourtail.bindlayers.BindLayer;
import xyz.wagyourtail.bindlayers.BindLayers;

import java.io.IOException;

import static net.minecraft.network.chat.Component.literal;
import static net.minecraft.network.chat.Component.translatable;

public class RenameLayerScreen extends Screen {
    public final Screen parent;
    public final String oldName;

    EditBox nameBox;

    public RenameLayerScreen(Screen parent, String layerName) {
        super(translatable("bindlayers.gui.rename_layer"));
        this.parent = parent;
        this.oldName = layerName;
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);

        drawCenteredString(poseStack, font, title, width / 2, 10, 0xFFFFFF);

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
        if (nameBox.getValue().equals(oldName)) {
            return;
        }
        if (BindLayers.INSTANCE.availableLayers().contains(nameBox.getValue())) {
            return;
        }
        BindLayer newLayer = BindLayers.INSTANCE.getOrCreate(nameBox.getValue());
        newLayer.copyFrom(BindLayers.INSTANCE.getOrCreate(oldName));
        if (BindLayers.INSTANCE.getActiveLayer().equals(oldName)) {
            BindLayers.INSTANCE.setActiveLayer(nameBox.getValue());
        }
        try {
            newLayer.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        BindLayer oldLayer = BindLayers.INSTANCE.removeLayer(oldName);
        oldLayer.removeFile();
    }

    @Override
    protected void init() {
        super.init();

        nameBox = addRenderableWidget(
            new EditBox(font, width / 2 - 100, height / 2 - 10, 200, 20, literal("New Name"))
        );
        nameBox.setFilter(s -> s.matches("[^#%&*+\\-/:;<=>?@\\[\\]^`{|}~\\\\]*"));
        nameBox.setMaxLength(32);
        nameBox.setBordered(true);
        nameBox.setTextColor(-1);
        nameBox.setValue(oldName);
        nameBox.setResponder(s -> {
            if (nameBox.getValue().length() == 0 || (BindLayers.INSTANCE.availableLayers().contains(s) && !s.equals(oldName))) {
                nameBox.setTextColor(0xFF0000);
            } else {
                nameBox.setTextColor(0xFFFFFF);
            }
        });

        addRenderableWidget(new Button(
            this.width / 2 - 50,
            this.height - 30,
            100,
            20,
            translatable("gui.done"),
            (b) -> onClose()
        ));

    }

}
