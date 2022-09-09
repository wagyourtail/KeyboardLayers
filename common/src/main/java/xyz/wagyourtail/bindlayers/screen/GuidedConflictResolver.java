package xyz.wagyourtail.bindlayers.screen;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import xyz.wagyourtail.bindlayers.BindLayer;
import xyz.wagyourtail.bindlayers.BindLayers;
import xyz.wagyourtail.bindlayers.screen.elements.LayerListWidget;
import xyz.wagyourtail.bindlayers.screen.elements.StringListWidget;

import java.util.*;
import java.util.stream.Collectors;

public class GuidedConflictResolver extends Screen {
    private final Map<String, BindLayer> newLayers = new Object2ObjectRBTreeMap<>();
    private final Screen parent;
    private Map<String, Set<String>> conflicts;
    private String currentSelected;

    private LayerListWidget layerList;
    private StringListWidget bindList;

    private Button cancel;
    private Button save;


    public GuidedConflictResolver(Screen parent) {
        super(Component.literal("Conflict Resolver"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        if (newLayers.isEmpty()) {
            firstInit();
        }

        layerList = addRenderableWidget(new LayerListWidget(this, minecraft, width / 2, height, 32, height - 62, newLayers::get));
        layerList.init(newLayers.keySet());
        bindList = addRenderableWidget(new StringListWidget(minecraft, width / 2, height / 2, 32, height / 2));
        bindList.setLeftPos(width / 2 + 10);

        // cancel
        cancel = addRenderableWidget(new Button(
            width / 2 - 100,
            height - 50,
            100,
            20,
            Component.translatable("gui.cancel"),
            (button) -> {
                assert minecraft != null;
                minecraft.setScreen(parent);
            }
        ));

        // save
        save = addRenderableWidget(new Button(
            width / 2,
            height - 50,
            100,
            20,
            Component.translatable("gui.done"),
            (button) -> {
                BindLayers.INSTANCE.defaultLayer.copyFrom(newLayers.get(BindLayers.INSTANCE.defaultLayer.name));
                for (BindLayer layer : newLayers.values()) {
                    BindLayer l = BindLayers.INSTANCE.getOrCreate(layer.name);
                    l.copyFrom(layer);
                }
                for (String layer : BindLayers.INSTANCE.availableLayers()) {
                    if (!newLayers.containsKey(layer)) {
                        BindLayer l = BindLayers.INSTANCE.removeLayer(layer);
                        l.removeFile();
                    }
                }
                assert minecraft != null;
                minecraft.options.save();
                minecraft.setScreen(parent);
            }
        ));

    }

    private void firstInit() {
        // get keys
        assert minecraft != null;
        KeyMapping[] mappings = minecraft.options.keyMappings;

        Map<String, Set<KeyMapping>> mappingsByCategory = new HashMap<>();
        for (KeyMapping mapping : mappings) {
            mappingsByCategory.computeIfAbsent(mapping.getCategory(), (c) -> new HashSet<>()).add(mapping);
        }

        Map<String, Set<InputConstants.Key>> usedKeysByCategory = new HashMap<>();
        for (KeyMapping mapping : mappings) {
            usedKeysByCategory.computeIfAbsent(mapping.getCategory(), (c) -> new HashSet<>()).add(mapping.getDefaultKey());
        }

        // compute conflicts
        conflicts = new HashMap<>();
        for (Map.Entry<String, Set<InputConstants.Key>> entry : usedKeysByCategory.entrySet()) {
            Set<String> conflictSet = new HashSet<>();
            for (Map.Entry<String, Set<InputConstants.Key>> entry2 : usedKeysByCategory.entrySet()) {
                if (entry.getKey().equals(entry2.getKey())) continue;
                for (InputConstants.Key key : entry.getValue()) {
                    if (entry2.getValue().contains(key)) {
                        conflictSet.add(entry2.getKey());
                        break;
                    }
                }
            }
            conflicts.put(entry.getKey(), conflictSet);
        }

        Set<String> defaultLayerCategories = Sets.newHashSet(
            "key.categories.movement",
            "key.categories.gameplay",
            "key.categories.inventory",
            "key.categories.creative",
            "key.categories.multiplayer",
            "key.categories.ui",
            "key.categories.misc"
        );

        // compute new layers
        BindLayer defaultLayer = new BindLayer(BindLayers.INSTANCE.defaultLayer.name, BindLayers.INSTANCE.defaultLayer.name);
        newLayers.put(defaultLayer.name, defaultLayer);

        // combine binds from vanilla categories
        defaultLayer.copyFromDefault(defaultLayerCategories.stream()
            .flatMap(e -> mappingsByCategory.get(e).stream())
            .toArray(KeyMapping[]::new));

        // add new layer for each remaining category
        for (Map.Entry<String, Set<KeyMapping>> entry : mappingsByCategory.entrySet()) {
            if (defaultLayerCategories.contains(entry.getKey())) continue;
            String translatedName = I18n.get(entry.getKey());
            // remove spaces and make pascal case
            String name = Arrays.stream(translatedName.split(" ")).map(e -> e.substring(0, 1).toUpperCase() + e.substring(1).toLowerCase()).collect(Collectors.joining());
            BindLayer layer = new BindLayer(name, translatedName);
            newLayers.put(layer.name, layer);
            layer.copyFromDefault(entry.getValue().toArray(new KeyMapping[0]));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean result = super.mouseClicked(mouseX, mouseY, button);
        updateSelected();
        return result;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean result = super.keyPressed(keyCode, scanCode, modifiers);
        updateSelected();
        return result;
    }

    public void updateSelected() {
        boolean update = false;
        if (layerList.getSelected() == null) {
            update = true;
            currentSelected = null;
        } else if (!currentSelected.equals(layerList.getSelected().layerName)) {
            update = true;
            currentSelected = layerList.getSelected().layerName;
        }
        if (update) {
            if (currentSelected == null) {
                bindList.init(new HashSet<>());
                for (GuiEventListener listener : children()) {
                    if (listener instanceof Button) {
                        ((Button) listener).active = false;
                    }
                }
            } else {
                bindList.init(new TreeSet<>(newLayers.get(currentSelected).binds.entrySet().stream().map(e -> e.getKey().getName() + ":" + e.getKey().saveString()).collect(Collectors.toSet())));
                for (GuiEventListener listener : children()) {
                    if (listener instanceof Button) {
                        ((Button) listener).active = false;
                    }
                }
            }
            save.active = true;
            cancel.active = true;
        }
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

}
