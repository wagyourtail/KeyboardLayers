package xyz.wagyourtail.bindlayers.mixin;

import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.wagyourtail.bindlayers.BindLayers;
import xyz.wagyourtail.bindlayers.screen.CreateLayerScreen;
import xyz.wagyourtail.bindlayers.screen.elements.DropDownWidget;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Mixin(KeyBindsScreen.class)
public class MixinKeyBindsScreen extends OptionsSubScreen {
    //IGNORE
    public MixinKeyBindsScreen(Screen screen, Options options, Component component) {
        super(screen, options, component);
    }

    @Inject(method = "init", at = @At("HEAD"))
    public void bindlayers$onInit(CallbackInfo ci) {
        // add layers dropdown to keybinds screen
        //        for (String availableLayer : BindLayers.INSTANCE.availableLayers()) {
        //            System.out.println(availableLayer);
        //        }
        Map<Component, String> layers = BindLayers.INSTANCE.availableLayers().stream().collect(Collectors.toMap(
            Component::literal,
            Function.identity()
        ));

        addRenderableWidget(new DropDownWidget(
            10,
            5,
            this.width / 4,
            12,
            () -> Component.literal(BindLayers.INSTANCE.getActiveLayer()),
            layers::keySet,
            (s) -> {
                String selectedLayer = layers.get(s);
                BindLayers.INSTANCE.setActiveLayer(selectedLayer);
            },
            () -> {
                assert minecraft != null;
                minecraft.setScreen(new CreateLayerScreen(this));
            }
        ));
    }

}
