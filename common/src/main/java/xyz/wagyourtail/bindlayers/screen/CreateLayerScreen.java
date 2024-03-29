package xyz.wagyourtail.bindlayers.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import xyz.wagyourtail.bindlayers.BindLayer;
import xyz.wagyourtail.bindlayers.BindLayers;
import xyz.wagyourtail.bindlayers.screen.elements.DropDownWidget;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.minecraft.network.chat.Component.literal;
import static net.minecraft.network.chat.Component.translatable;
import static xyz.wagyourtail.bindlayers.BindLayers.INSTANCE;

public class CreateLayerScreen extends Screen {
    private final Screen parent;

    private EditBox nameField;

    private BindLayer parentLayer = BindLayers.INSTANCE.vanillaLayer;

    public CreateLayerScreen(Screen parent) {
        super(translatable("bindlayers.gui.create_layer"));
        this.parent = parent;
    }

    @Override
    public void render(@NotNull PoseStack stack, int mouseX, int mouseY, float delta) {
        renderBackground(stack);

        drawCenteredString(stack, font, title, width / 2, 10, 0xFFFFFF);

        super.render(stack, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }

    @Override
    protected void init() {
        super.init();
        nameField = new EditBox(font, width / 2 - 100, height / 2 - 10, 200, 20, literal("Name"));
        nameField.setMaxLength(32);
        nameField.setBordered(true);
        nameField.setTextColor(-1);
        nameField.setValue("");
        nameField.setFilter(s -> s.matches("[^#%&*+\\-/:;<=>?@\\[\\]^`{|}~\\\\]*"));
        addRenderableWidget(nameField);

        Map<Component, String> layers = BindLayers.INSTANCE.availableLayers().stream().collect(Collectors.toMap(
            Component::literal,
            Function.identity()
        ));

        addRenderableWidget(new DropDownWidget(
            width / 2 - 100,
            height / 2 + 15,
            200,
            12,
            () -> literal(parentLayer.name),
            layers::keySet,
            (s) -> parentLayer = BindLayers.INSTANCE.getOrCreate(layers.get(s)),
            null
        ));


        addRenderableWidget(new Button.Builder(
            translatable("bindlayers.gui.create"),
            (button) -> {
                if (!nameField.getValue().isEmpty()) {
                    BindLayer l = INSTANCE.getOrCreate(nameField.getValue());
                    l.setParentLayer(parentLayer.name);
                    onClose();
                }
            }
            ).bounds(
                width / 2 - 205,
                height - 30,
                200,
                20
            ).build()
        );

        addRenderableWidget(new Button.Builder(
            translatable("gui.cancel"),
            (button) -> {
                onClose();
            }
        ).bounds(
            width / 2 + 5,
            height - 30,
            200,
            20).build());
    }

}
