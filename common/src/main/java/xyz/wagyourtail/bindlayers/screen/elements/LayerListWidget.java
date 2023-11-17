package xyz.wagyourtail.bindlayers.screen.elements;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import xyz.wagyourtail.bindlayers.BindLayer;
import xyz.wagyourtail.bindlayers.BindLayers;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static xyz.wagyourtail.bindlayers.legacy.ComponentHelper.literal;

public class LayerListWidget extends ObjectSelectionList<LayerListWidget.LayerEntry> {
    public static final int ENTRY_HEIGHT = 25;
    public final Font font;
    public final Screen parent;
    public final Function<String, BindLayer> getLayer;

    public LayerListWidget(Screen parent, Minecraft minecraft, int width, int height, int top, int bottom, Function<String, BindLayer> getLayer) {
        super(minecraft, width, height, top, bottom, ENTRY_HEIGHT);
        font = minecraft.font;
        this.parent = parent;
        this.getLayer = getLayer;
    }

    @Override
    public int getRowWidth() {
        return width;
    }

    protected int getScrollbarPosition() {
        return this.x1 - 6;
    }

    @Override
    public boolean removeEntry(@NotNull LayerEntry entry) {
        return super.removeEntry(entry);
    }

    public void init(Set<String> availableLayers) {
        init(availableLayers, true);
    }

    public void init(Set<String> availableLayers, boolean skipDefault) {
        clearEntries();
        if (!skipDefault) {
            if (availableLayers.contains(BindLayers.INSTANCE.vanillaLayer.name)) {
                addEntry(new LayerEntry(BindLayers.INSTANCE.vanillaLayer.name));
            }
        }
        for (String layer : availableLayers) {
            if (Objects.equals(layer, BindLayers.INSTANCE.vanillaLayer.name)) {
                continue;
            }
            addEntry(new LayerEntry(layer));
        }
    }

    public void setSelected(String layer) {
        for (LayerEntry entry : children()) {
            if (Objects.equals(entry.layer.name, layer)) {
                setSelected(entry);
                return;
            }
        }
    }

    public class LayerEntry extends ObjectSelectionList.Entry<LayerEntry> {
        public final String layerName;
        public final BindLayer layer;

        public LayerEntry(String layer) {
            this.layerName = layer;
            this.layer = getLayer.apply(layer);
        }

        @Override
        public Component getNarration() {
            return literal("");
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            setSelected(this);
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void render(@NotNull PoseStack poseStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            // draw the layer name top left
            drawString(poseStack, font, layerName, left + 10, top, 0xFFFFFF);
            // parent name below
            drawString(poseStack, font, literal(layer.getParentLayer())
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), left + 10, top + 10, 0xFFFFFF);
        }

    }

}
