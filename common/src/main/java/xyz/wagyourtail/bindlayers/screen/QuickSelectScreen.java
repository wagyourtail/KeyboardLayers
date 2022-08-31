package xyz.wagyourtail.bindlayers.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import xyz.wagyourtail.bindlayers.BindLayers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class QuickSelectScreen extends Screen {
    String searchString = "";
    List<String> matches = new ArrayList<>();
    int hilightedResult = 0;

    public QuickSelectScreen() {
        super(Component.translatable("bindlayers.quick_select"));
    }

    @Override
    public boolean charTyped(char c, int i) {
        searchString += c;
        return true;
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int i, int j, float f) {
        int boxHeight = 12 + matches.size() * 12;
        fill(poseStack, width / 2 - 100, height / 4, width / 2 + 100, height / 4 + boxHeight, 0x4F000000);
        // draw search string
        drawCenteredString(poseStack, font, searchString, width / 2, height / 4 + 1, 0xFFFFFF);
        fill(poseStack, width / 2 - 95, height / 4 + 11, width / 2 + 100, height / 4 + 12, 0xFFFFFFFF);

        // draw hilightbox
        fill(
            poseStack,
            width / 2 - 100,
            height / 4 + 12 + 11 * hilightedResult,
            width / 2 + 100,
            height / 4 + 23 + 11 * hilightedResult,
            0x4F7F7F7F
        );

        int resultIndex = 0;
        for (String result : matches) {
            drawString(
                poseStack,
                font,
                result,
                width / 2 - 90,
                height / 4 + 13 + 11 * resultIndex++,
                resultIndex == 5 ? 0xFF7F7F7F : 0xFFFFFF
            );
        }

        super.render(poseStack, i, j, f);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        switch (i) {
            case GLFW.GLFW_KEY_BACKSPACE:
                if (searchString.length() > 0) {
                    searchString = searchString.substring(0, searchString.length() - 1);
                }
                return true;
            case GLFW.GLFW_KEY_DOWN:
                if (hilightedResult < matches.size() - 1 && hilightedResult < 5) {
                    hilightedResult++;
                }
                return true;
            case GLFW.GLFW_KEY_UP:
                if (hilightedResult > 0) {
                    hilightedResult--;
                }
                return true;
            case GLFW.GLFW_KEY_ENTER:
                if (hilightedResult != -1) {
                    BindLayers.INSTANCE.setActiveLayer(matches.get(hilightedResult));
                }
                onClose();
                return true;
            default:
        }
        return super.keyPressed(i, j, k);
    }

    @Override
    protected void init() {
        super.init();
        searchString = "";
        onSearchChanged();
    }

    public void onSearchChanged() {
        matches.clear();
        hilightedResult = -1;
        for (String layer : BindLayers.INSTANCE.availableLayers()) {
            if (Objects.equals(layer, BindLayers.INSTANCE.getActiveLayer())) {
                continue;
            }
            if (layer.contains(searchString)) {
                matches.add(layer);
                if (matches.size() >= 5) {
                    matches.add("...");
                    hilightedResult = 0;
                    return;
                }
            }
        }
        if (matches.size() > 0) {
            hilightedResult = 0;
        }
    }

}
