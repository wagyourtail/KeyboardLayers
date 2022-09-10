package xyz.wagyourtail.bindlayers.screen.elements;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;

import java.util.Set;

public class StringListWidget extends ObjectSelectionList<StringListWidget.StringEntry> {
    private static final int ENTRY_HEIGHT = 12;

    public StringListWidget(Minecraft minecraft, int width, int height, int top, int bottom) {
        super(minecraft, width, height, top, bottom, ENTRY_HEIGHT);
    }

    protected int getScrollbarPosition() {
        return this.x1 - 6;
    }

    public int addEntry(String entry) {
        return addEntry(new StringEntry(entry));
    }

    public void init(Set<String> strings) {
        clearEntries();
        for (String s : strings) {
            addEntry(s);
        }
    }

    @Override
    public int getRowWidth() {
        return width;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    public class StringEntry extends ObjectSelectionList.Entry<StringEntry> {
        public final String string;

        public StringEntry(String string) {
            this.string = string;
        }

        @Override
        public void render(PoseStack poseStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            drawString(poseStack, Minecraft.getInstance().font, string, left + 10, top, 0xFFFFFF);
        }

        @Override
        public Component getNarration() {
            return null;
        }

    }
}
