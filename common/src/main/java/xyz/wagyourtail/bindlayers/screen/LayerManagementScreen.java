package xyz.wagyourtail.bindlayers.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import org.jetbrains.annotations.NotNull;
import xyz.wagyourtail.bindlayers.BindLayer;
import xyz.wagyourtail.bindlayers.BindLayers;
import xyz.wagyourtail.bindlayers.screen.elements.LayerListWidget;

import java.util.Set;

import static java.lang.String.join;
import static net.minecraft.network.chat.Component.literal;
import static net.minecraft.network.chat.Component.translatable;
import static xyz.wagyourtail.bindlayers.BindLayers.INSTANCE;

public class LayerManagementScreen extends Screen {
    Screen parent;

    LayerListWidget layerList;
    Button back;

    public LayerManagementScreen(Screen parent) {
        super(literal("BindLayers"));
        this.parent = parent;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean click = super.mouseClicked(mouseX, mouseY, button);
        updateButtons();
        return click;
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int i, int j, float f) {
        renderBackground(poseStack);

        super.render(poseStack, i, j, f);

        drawCenteredString(poseStack, font, title, width / 2, 10, 0xFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean press = super.keyPressed(keyCode, scanCode, modifiers);
        updateButtons();
        return press;
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
        minecraft.options.save();
    }

    @Override
    protected void init() {
        super.init();

        layerList = addRenderableWidget(
            new LayerListWidget(this, minecraft, width, height, 32, height - 32, BindLayers.INSTANCE::getOrCreate)
        );

        layerList.init(BindLayers.INSTANCE.availableLayers());

        // back
        back = addRenderableWidget(new Button.Builder(
            translatable("bindlayers.gui.back"),
            button -> {
                onClose();
            }
        ).bounds(
            width / 2 - 210,
            height - 30,
            80,
            20).build());

        // delete
        addRenderableWidget(new Button.Builder(
            translatable("bindlayers.gui.delete"),
            button -> {
                assert layerList.getSelected() != null;
                Set<BindLayer> childLayers = INSTANCE.getChildLayers(layerList.getSelected().layerName);
                if (childLayers.isEmpty()) {
                    BindLayer layer = INSTANCE.removeLayer(layerList.getSelected().layerName);
                    if (layer != null) {
                        layer.removeFile();
                    }
                    layerList.removeEntry(layerList.getSelected());
                } else {
                    assert minecraft != null;
                    minecraft.setScreen(new ConfirmScreen(
                        (bool) -> {
                            if (bool) {
                                childLayers.stream().map(l -> l.name).forEach(INSTANCE::removeLayer);
                                BindLayer layer = INSTANCE.removeLayer(layerList.getSelected().layerName);
                                if (layer != null) {
                                    layer.removeFile();
                                }
                                layerList.removeEntry(layerList.getSelected());
                            }
                            minecraft.setScreen(null);
                        },
                        translatable("bindlayers.layer.confirm_delete"),
                        translatable("bindlayers.layer.confirm_delete2").append(
                            "\n" + join(", ", childLayers.stream().map(e -> e.name).toArray(String[]::new)))
                    ));
                    INSTANCE.removeLayer(layerList.getSelected().layerName);
                }
            }
        ).bounds(
            width / 2 - 125,
            height - 30,
            80,
            20).build());
        // edit
        addRenderableWidget(new Button.Builder(
            translatable("bindlayers.gui.edit"),
            button -> {
                assert layerList.getSelected() != null;
                INSTANCE.setActiveLayer(layerList.getSelected().layerName);
                assert minecraft != null;
                minecraft.setScreen(new KeyBindsScreen(this, minecraft.options));
            }
        ).bounds(
            width / 2 - 40,
            height - 30,
            80,
            20).build());
        // rename
        addRenderableWidget(new Button.Builder(
            translatable("bindlayers.gui.rename"),
            button -> {
                assert minecraft != null;
                assert layerList.getSelected() != null;
                minecraft.setScreen(new RenameLayerScreen(this, layerList.getSelected().layerName));
            }
        ).bounds(
            width / 2 + 45,
            height - 30,
            80,
            20).build());

        // change parent
        addRenderableWidget(new Button.Builder(
            translatable("bindlayers.gui.change_parent"),
            button -> {
                assert minecraft != null;
                assert layerList.getSelected() != null;
                minecraft.setScreen(new ChangeParentScreen(this, layerList.getSelected().layerName));
            }
        ).bounds(
            width / 2 + 130,
            height - 30,
            80,
            20).build());

        updateButtons();
    }

    public void updateButtons() {
        if (layerList.getSelected() != null) {
            for (GuiEventListener b : this.children()) {
                if (b instanceof Button) {
                    ((Button) b).active = true;
                }
            }
        } else {
            for (GuiEventListener b : this.children()) {
                if (b instanceof Button) {
                    ((Button) b).active = false;
                }
            }
            back.active = true;
        }
    }


}
