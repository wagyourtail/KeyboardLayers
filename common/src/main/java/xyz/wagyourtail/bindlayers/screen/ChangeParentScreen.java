package xyz.wagyourtail.bindlayers.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import xyz.wagyourtail.bindlayers.BindLayer;
import xyz.wagyourtail.bindlayers.BindLayers;
import xyz.wagyourtail.bindlayers.screen.elements.DropDownWidget;

import java.util.*;
import java.util.stream.Collectors;

public class ChangeParentScreen extends Screen {
    public final Screen parent;
    public final String layerName;
    public final BindLayer layer;
    String loopLayer;
    boolean warnLoop = false;
    public final List<Component> parents = new ArrayList<>();

    public ChangeParentScreen(Screen parent, String layerName) {
        super(Component.literal("Change Parent"));
        this.parent = parent;
        this.layerName = layerName;
        this.layer = BindLayers.INSTANCE.getOrCreate(layerName);
    }

    @Override
    protected void init() {
        super.init();
        Map<Component, String> layers = new LinkedHashMap<>();

        for (String layer : BindLayers.INSTANCE.availableLayers()) {
            layers.put(Component.literal(layer), layer);
        }

        addRenderableWidget(new DropDownWidget(
            10,
            5,
            this.width / 4,
            12,
            () -> Component.literal(BindLayers.INSTANCE.getActiveLayer()),
            () -> layers.keySet().stream().filter(c -> !c.getString().equals(layerName)).collect(Collectors.toSet()),
            (s) -> {
                String selectedLayer = layers.get(s);
                layer.setParentLayer(selectedLayer);
                checkLoop();
            },
            null
        ));

        addRenderableWidget(new Button(
            10,
            20,
            this.width / 4,
            20,
            Component.literal("Done"),
            (b) -> {
                assert minecraft != null;
                minecraft.setScreen(parent);
            }
        ));

        checkLoop();
    }

    public void checkLoop() {
        parents.clear();
        Set<String> parents = new HashSet<>();
        String parent = layer.name;
        warnLoop = false;
        while (!parent.equals(BindLayers.INSTANCE.defaultLayer.name)) {
            if (parents.contains(layerName)) {
                warnLoop = true;
                break;
            }
            parents.add(parent);
            parent = BindLayers.INSTANCE.getOrCreate(parent).getParentLayer();
        }
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);

        drawCenteredString(poseStack, font, title, width / 2, 10, 0xFFFFFF);

        if (loopLayer != null) {
            drawCenteredString(
                poseStack,
                font,
                "Warning: This will create a parent layer loop!",
                width / 2,
                height / 2,
                0xFF0000
            );
        }
        int i = 2;
        for (Component parent : parents) {
            drawCenteredString(poseStack, font, parent.getString(), width / 2, height / 2 + (i * 10), 0xFFFFFF);
            i++;
        }
        if (loopLayer != null) {
            drawCenteredString(
                poseStack,
                font,
                loopLayer,
                width / 2,
                height / 2 + (i * 10),
                0xFF0000
            );
        }

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

}
