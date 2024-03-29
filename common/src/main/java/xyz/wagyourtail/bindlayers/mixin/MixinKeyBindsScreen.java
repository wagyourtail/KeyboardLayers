package xyz.wagyourtail.bindlayers.mixin;

import com.blamejared.controlling.client.NewKeyBindsScreen;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.wagyourtail.bindlayers.BindLayers;
import xyz.wagyourtail.bindlayers.screen.CreateLayerScreen;
import xyz.wagyourtail.bindlayers.screen.GuidedConflictResolver;
import xyz.wagyourtail.bindlayers.screen.elements.DropDownWidget;

import java.util.LinkedHashMap;
import java.util.Map;

import static net.minecraft.network.chat.Component.literal;
import static net.minecraft.network.chat.Component.translatable;
import static xyz.wagyourtail.bindlayers.BindLayers.INSTANCE;

@Mixin(KeyBindsScreen.class)
public class MixinKeyBindsScreen extends OptionsSubScreen {

    @Unique
    DropDownWidget bindlayers$layerSelector;

    //IGNORE
    public MixinKeyBindsScreen(Screen screen, Options options, Component component) {
        super(screen, options, component);
    }

    @Inject(method = "init", at = @At("HEAD"))
    public void bindlayers$onInit(CallbackInfo ci) {
        // add layers dropdown to keybinds screen
        Map<Component, String> layers = new LinkedHashMap<>();

        for (String layer : BindLayers.INSTANCE.availableLayers()) {
            layers.put(literal(layer), layer);
        }

        bindlayers$layerSelector = addRenderableWidget(new DropDownWidget(
            10,
            5,
            75,
            12,
            () -> literal(INSTANCE.getActiveLayer()),
            layers::keySet,
            (s) -> {
                String selectedLayer = layers.get(s);
                BindLayers.INSTANCE.setActiveLayer(selectedLayer, true);
            },
            () -> {
                assert minecraft != null;
                minecraft.setScreen(new CreateLayerScreen(this));
            }
        ));

        addRenderableWidget(new Button.Builder(
            translatable("bindlayers.gui.layer_generator"),
            (button) -> {
                assert minecraft != null;
                minecraft.setScreen(new GuidedConflictResolver(this));
            }
        ).bounds(
            90,
            5,
            100,
            12)
        .build());
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void bindlayers$onMouseClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (bindlayers$layerSelector.mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

}
