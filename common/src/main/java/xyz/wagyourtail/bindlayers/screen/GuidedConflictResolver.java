package xyz.wagyourtail.bindlayers.screen;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.wagyourtail.bindlayers.BindLayer;
import xyz.wagyourtail.bindlayers.BindLayers;
import xyz.wagyourtail.bindlayers.screen.elements.DropDownWidget;
import xyz.wagyourtail.bindlayers.screen.elements.LayerListWidget;
import xyz.wagyourtail.bindlayers.screen.elements.StringListWidget;

import java.util.*;
import java.util.stream.Collectors;

public class GuidedConflictResolver extends Screen {
    private final Map<String, BindLayer> newLayers = new Object2ObjectRBTreeMap<>();
    private final Screen parent;
    private Map<String, Set<String>> conflicts;
    @Nullable
    private String currentSelected;

    private LayerListWidget layerList;
    private StringListWidget bindList;

    private Button cancel;
    private Button save;


    public GuidedConflictResolver(Screen parent) {
        super(Component.translatable("bindlayers.gui.layer_generator"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        if (newLayers.isEmpty()) {
            firstInit();
        }

        layerList = addRenderableWidget(new LayerListWidget(this, minecraft, width / 2 - 10, height, 32, height - 32, newLayers::get));
        layerList.setLeftPos(5);
        layerList.init(newLayers.keySet(), false);
        bindList = addRenderableWidget(new StringListWidget(minecraft, width / 2 - 10, height / 2, 32, height / 2));
        bindList.setLeftPos(width / 2 + 5);

        // cancel
        cancel = addRenderableWidget(new Button(
            width / 2 - 100,
            height - 30,
            100,
            20,
            Component.translatable("gui.cancel"),
            (button) -> {
                onClose();
            }
        ));

        // save
        save = addRenderableWidget(new Button(
            width / 2,
            height - 30,
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
                onClose();
            }
        ));

        // right middle
        // rename box
        assert minecraft != null;
        EditBox renameBox = addRenderableWidget(new EditBox(
            minecraft.font,
            width / 2 + 10,
            height / 2 + 10,
            95,
            12,
            Component.translatable("bindlayers.gui.rename")
        ));

        renameBox.setMaxLength(32);
        renameBox.setBordered(true);
        renameBox.setTextColor(-1);
        renameBox.setValue("");
        renameBox.setFilter(s -> s.matches("[^#%&*+\\-/:;<=>?@\\[\\]^`{|}~\\\\]+"));
        renameBox.setResponder(s -> {
            if (newLayers.containsKey(s) && !s.equals(currentSelected)) {
                renameBox.setTextColor(0xFF0000);
            } else {
                renameBox.setTextColor(0xFFFFFF);
            }
        });

        // rename button
        addRenderableWidget(new Button(
            width / 2 + 137,
            height / 2 + 10,
            50,
            12,
            Component.translatable("bindlayers.gui.rename"),
            (button) -> {
                if (currentSelected != null && !renameBox.getValue().isEmpty()) {
                    BindLayer layer = newLayers.get(currentSelected);
                    if (newLayers.containsKey(renameBox.getValue())) return;
                    newLayers.remove(currentSelected);
                    BindLayer renamed = new BindLayer(renameBox.getValue(), layer.getParentLayer());
                    renamed.copyFrom(layer);
                    newLayers.put(renameBox.getValue(), renamed);
                    layerList.init(newLayers.keySet(), false);
                    layerList.setSelected(renamed.name);
                }
            }
        ));

        Map<Component, String> allLayers = new LinkedHashMap<>();
        final String[] localCurrentParent = { null };

        // change parent dropdown
        addRenderableWidget(new DropDownWidget(
            width / 2 + 125,
            height / 2 + 25,
            75,
            12,
            () -> Component.literal(currentSelected == null ? I18n.get("bindlayers.gui.none") : currentSelected),
            () -> {
                if (!Objects.equals(localCurrentParent[0], currentSelected)) {
                    allLayers.clear();
                    localCurrentParent[0] = currentSelected;
                    if (!Objects.equals(localCurrentParent[0], BindLayers.INSTANCE.defaultLayer.name)) {
                        newLayers.keySet().stream().filter(s -> s.equals(localCurrentParent[0])).forEach(s -> allLayers.put(Component.literal(s), s));
                    }
                }
                Set<Component> ret = new LinkedHashSet<>();
                ret.add(Component.translatable("bindlayers.gui.none"));
                ret.addAll(allLayers.keySet());
                return ret;
            },
            (c) -> {
                if (currentSelected != null) {
                    BindLayer layer = newLayers.get(currentSelected);
                    String l = allLayers.get(c);
                    if (l != null)
                        layer.setParentLayer(l);
                }
            },
            null
        ));

        Map<Component, String> nonConflictLayers = new LinkedHashMap<>();
        final String[] localCurrentMerge = {currentSelected};

        // merge dropdown
        addRenderableWidget(new DropDownWidget(
            width / 2 + 125,
            height / 2 + 40,
            75,
            12,
            () -> Component.translatable("bindlayers.gui.none"),
            () -> {
                if (!Objects.equals(localCurrentMerge[0], currentSelected)) {
                    nonConflictLayers.clear();
                    localCurrentMerge[0] = currentSelected;
                    newLayers.keySet().stream().filter(s -> !s.equals(BindLayers.INSTANCE.defaultLayer.name) && !conflicts.computeIfAbsent(currentSelected, c -> new HashSet<>()).contains(s)).forEach(s -> nonConflictLayers.put(Component.literal(s), s));
                }
                Set<Component> ret = new LinkedHashSet<>();
                ret.add(Component.translatable("bindlayers.gui.none"));
                ret.addAll(nonConflictLayers.keySet());
                return ret;
            },
            (c) -> {
                if (currentSelected != null) {
                    BindLayer layer = newLayers.get(currentSelected);
                    BindLayer merge = newLayers.get(nonConflictLayers.get(c));
                    if (merge == null) return;
                    layer.addAll(merge);
                    newLayers.remove(nonConflictLayers.get(c));
                    for (BindLayer l : newLayers.values()) {
                        if (l.getParentLayer().equals(nonConflictLayers.get(c))) {
                            l.setParentLayer(currentSelected);
                        }
                    }
                    layerList.init(newLayers.keySet(), false);
                    layerList.setSelected(layer.name);
                }
            },
            null
        ));

        updateSelected();

    }

    private void firstInit() {
        // get keys
        assert minecraft != null;
        KeyMapping[] mappings = minecraft.options.keyMappings;

        Map<String, Set<KeyMapping>> mappingsByCategory = new HashMap<>();
        for (KeyMapping mapping : mappings) {
            mappingsByCategory.computeIfAbsent(mapping.getCategory(), (c) -> new HashSet<>()).add(mapping);
        }

        Map<String, Set<BindLayer.Bind>> usedKeysByCategory = new HashMap<>();
        for (KeyMapping mapping : mappings) {
            usedKeysByCategory.computeIfAbsent(mapping.getCategory(), (c) -> new HashSet<>()).add(BindLayers.provider.keyMappingDefaultToBind(mapping));
        }

        // compute conflicts
        conflicts = new HashMap<>();
        for (Map.Entry<String, Set<BindLayer.Bind>> entry : usedKeysByCategory.entrySet()) {
            Set<String> conflictSet = new HashSet<>();
            for (Map.Entry<String, Set<BindLayer.Bind>> entry2 : usedKeysByCategory.entrySet()) {
                if (entry.getKey().equals(entry2.getKey())) continue;
                for (BindLayer.Bind key : entry.getValue()) {
                    if (entry2.getValue().contains(key) && key.key != InputConstants.UNKNOWN) {
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

        for (KeyMapping mapping : mappings) {
            if (defaultLayer.binds.containsKey(mapping)) continue;
            defaultLayer.binds.put(mapping, new BindLayer.Bind(InputConstants.UNKNOWN, 0));
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
        } else if (!Objects.equals(currentSelected, layerList.getSelected().layerName)) {
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
                    if (listener instanceof EditBox) {
                        ((EditBox) listener).setEditable(false);
                        ((EditBox) listener).setValue("");
                    }
                }
            } else {
                bindList.init(new TreeSet<>(newLayers.get(currentSelected).binds.keySet().stream().map(bind ->
                    I18n.get(bind.getName()) + "  -  " + I18n.get(bind.saveString())).collect(Collectors.toSet())));
                for (GuiEventListener listener : children()) {
                    if (listener instanceof Button) {
                        ((Button) listener).active = true;
                    }
                    if (listener instanceof EditBox) {
                        ((EditBox) listener).setEditable(true);
                        ((EditBox) listener).setValue(currentSelected);
                    }
                }
            }
            save.active = true;
            cancel.active = true;
        }
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);

        // draw string labeling parent
        drawString(poseStack, font, I18n.get("bindlayers.gui.parent"), width / 2 + 10, height / 2 + 25, 0xFFFFFF);

        // draw string labeling merge
        drawString(poseStack, font, I18n.get("bindlayers.gui.merge"), width / 2 + 10, height / 2 + 40, 0xFFFFFF);


        super.render(poseStack, mouseX, mouseY, partialTick);
    }

}
