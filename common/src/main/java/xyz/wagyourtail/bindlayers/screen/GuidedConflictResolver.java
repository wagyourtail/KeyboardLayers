package xyz.wagyourtail.bindlayers.screen;

import com.google.common.collect.Sets;
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

import static net.minecraft.client.resources.language.I18n.get;
import static net.minecraft.network.chat.Component.literal;
import static net.minecraft.network.chat.Component.translatable;
import static xyz.wagyourtail.bindlayers.BindLayers.INSTANCE;

public class GuidedConflictResolver extends Screen {
    private final Map<String, BindLayer> newLayers = new Object2ObjectRBTreeMap<>();
    private final Screen parent;
    private Map<BindLayer, Set<BindLayer>> conflicts;
    @Nullable
    private String currentSelected;

    private LayerListWidget layerList;
    private StringListWidget bindList;

    private Button cancel;
    private Button save;


    public GuidedConflictResolver(Screen parent) {
        super(translatable("bindlayers.gui.layer_generator"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }

    private Set<String> getParents(@NotNull BindLayer current) {
        Set<String> parents = new LinkedHashSet<>();
        BindLayer parent = newLayers.get(current.getParentLayer());
        parents.add(current.name);
        while (parent != newLayers.get(BindLayers.INSTANCE.vanillaLayer.name)) {
            if (!parents.add(parent.name)) {
                break;
            }
            parent = newLayers.get(parent.getParentLayer());
        }
        parents.add(BindLayers.INSTANCE.vanillaLayer.name);
        parents.remove(current.name);
        return parents;
    }

    private void maskParent(@NotNull BindLayer layer) {
        Set<BindLayer.Bind> binds = new HashSet<>(layer.binds.values());
        binds.remove(BindLayer.Bind.UNKNOWN);

        BindLayer parent = newLayers.get(layer.getParentLayer());
        if (parent == layer) {
            return;
        }
        for (Map.Entry<KeyMapping, BindLayer.Bind> e : parent.binds.entrySet()) {
            if (binds.contains(e.getValue())) {
                layer.binds.put(e.getKey(), BindLayer.Bind.UNKNOWN);
            }
        }
    }

    private void remaskChildren(@NotNull BindLayer layer) {
        remaskChildren(layer, Sets.newHashSet(layer));
    }

    private void remaskChildren(@NotNull BindLayer layer, Set<BindLayer> seenLayers) {
        for (BindLayer l : newLayers.values()) {
            if (l.getParentLayer().equals(layer.name) && l != layer) {
                if (!seenLayers.contains(l)) {
                    seenLayers.add(l);
                    unmaskParent(l);
                    maskParent(l);
                    remaskChildren(l, seenLayers);
                }
            }
        }
    }

    private void unmaskParent(@NotNull BindLayer layer) {
        Set<BindLayer.Bind> binds = new HashSet<>(layer.binds.values());
        binds.remove(BindLayer.Bind.UNKNOWN);
        Set<KeyMapping> unknowns = layer.binds.entrySet().stream().filter(e -> e.getValue()
            .equals(BindLayer.Bind.UNKNOWN)).map(Map.Entry::getKey).collect(Collectors.toSet());

        BindLayer parent = newLayers.get(layer.getParentLayer());
        if (parent == layer) {
            return;
        }
        for (Map.Entry<KeyMapping, BindLayer.Bind> e : parent.binds.entrySet()) {
            if (binds.contains(e.getValue()) && unknowns.contains(e.getKey())) {
                layer.binds.remove(e.getKey());
            }
        }
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
            usedKeysByCategory.computeIfAbsent(mapping.getCategory(), (c) -> new HashSet<>())
                .add(BindLayers.provider.keyMappingDefaultToBind(mapping));
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
        BindLayer defaultLayer = new BindLayer(
            BindLayers.INSTANCE.vanillaLayer.name,
            BindLayers.INSTANCE.vanillaLayer.name
        );
        newLayers.put(defaultLayer.name, defaultLayer);

        // combine binds from vanilla categories
        defaultLayer.copyFromDefault(defaultLayerCategories.stream()
            .flatMap(e -> mappingsByCategory.getOrDefault(e, new HashSet<>()).stream())
            .toArray(KeyMapping[]::new));

        // add new layer for each remaining category
        for (Map.Entry<String, Set<KeyMapping>> entry : mappingsByCategory.entrySet()) {
            if (defaultLayerCategories.contains(entry.getKey())) {
                continue;
            }
            String translatedName = I18n.get(entry.getKey());
            // remove spaces and make pascal case
            String name = Arrays.stream(translatedName.split(" ")).map(e -> e.substring(0, 1).toUpperCase() +
                e.substring(1).toLowerCase()).collect(Collectors.joining());
            BindLayer layer = new BindLayer(name, defaultLayer.name);
            maskParent(layer);
            remaskChildren(layer);
            newLayers.put(layer.name, layer);
            layer.copyFromDefault(entry.getValue().toArray(new KeyMapping[0]));
        }


        conflicts = new HashMap<>();
        for (BindLayer layer : newLayers.values()) {
            for (BindLayer other : newLayers.values()) {
                if (layer.equals(other)) {
                    continue;
                }
                if (layer.binds.values().stream().anyMatch(e -> other.binds.containsValue(e) && !e.equals(BindLayer.Bind.UNKNOWN))) {
                    conflicts.computeIfAbsent(layer, (k) -> new HashSet<>()).add(other);
                }
            }
        }

        for (KeyMapping mapping : mappings) {
            if (defaultLayer.binds.containsKey(mapping)) {
                continue;
            }
            defaultLayer.binds.put(mapping, BindLayer.Bind.UNKNOWN);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean result = super.mouseClicked(mouseX, mouseY, button);
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
                bindList.initWithComponent(new LinkedHashSet<>(newLayers.get(currentSelected).binds.entrySet()
                    .stream()
                    .map(bind ->
                        translatable(bind.getKey().getName())
                            .append("  -  ")
                            .append(bind.getValue().displayName()))
                    .collect(Collectors.toList())));
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
        drawCenteredString(poseStack, font, translatable("bindlayers.gui.parent"), width / 2 + 55, height / 2 + 40, 0xFFFFFF);

        // draw string labeling merge
        drawCenteredString(poseStack, font, translatable("bindlayers.gui.merge"), width / 2 + 155, height / 2 + 40, 0xFFFFFF);

        // draw string labeling force-merge
        drawCenteredString(poseStack, font, translatable("bindlayers.gui.force_merge"), width / 2 + 55, height / 2 + 65, 0xFFFFFF);

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean result = super.keyPressed(keyCode, scanCode, modifiers);
        updateSelected();
        return result;
    }

    @Override
    protected void init() {
        super.init();
        if (newLayers.isEmpty()) {
            firstInit();
        }

        layerList = addRenderableWidget(new LayerListWidget(
            this,
            minecraft,
            width / 2 - 10,
            height,
            32,
            height - 32,
            newLayers::get
        ));
        layerList.setLeftPos(5);
        layerList.init(newLayers.keySet(), false);
        bindList = addRenderableWidget(new StringListWidget(minecraft, width / 2 - 10, height / 2, 32, height / 2));
        bindList.setLeftPos(width / 2 + 5);

        // right middle
        // rename box
        assert minecraft != null;
        EditBox renameBox = addRenderableWidget(new EditBox(
            minecraft.font,
            width / 2 + 5,
            height / 2 + 10,
            200,
            12,
            translatable("bindlayers.gui.rename")
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
            width / 2 + 55,
            height / 2 + 25,
            100,
            12,
            translatable("bindlayers.gui.rename"),
            (button) -> {
                if (currentSelected != null && !renameBox.getValue().isEmpty()) {
                    BindLayer layer = newLayers.get(currentSelected);
                    Set<BindLayer> conflict = conflicts.get(layer);
                    if (newLayers.containsKey(renameBox.getValue())) {
                        return;
                    }
                    conflicts.remove(newLayers.remove(currentSelected));
                    BindLayer renamed = new BindLayer(renameBox.getValue(), layer.getParentLayer());
                    renamed.copyFrom(layer);
                    newLayers.put(renameBox.getValue(), renamed);
                    conflicts.put(renamed, conflict);
                    conflicts.values().forEach(set -> {
                        if (set.contains(layer)) {
                            set.remove(layer);
                            set.add(renamed);
                        }
                    });
                    layerList.init(newLayers.keySet(), false);
                    layerList.setSelected(renamed.name);
                }
            }
        ));

        Map<Component, String> allLayers = new LinkedHashMap<>();
        final String[] localCurrentParent = {null};

        // change parent dropdown
        addRenderableWidget(new DropDownWidget(
            width / 2 + 5,
            height / 2 + 50,
            95,
            12,
            () -> literal(currentSelected == null ? get("gui.cancel") : currentSelected),
            () -> {
                if (!Objects.equals(localCurrentParent[0], currentSelected)) {
                    allLayers.clear();
                    localCurrentParent[0] = currentSelected;
                    if (!Objects.equals(localCurrentParent[0], BindLayers.INSTANCE.vanillaLayer.name)) {
                        newLayers.keySet()
                            .stream()
                            .filter(s -> !s.equals(localCurrentParent[0]) && !getParents(newLayers.get(s)).contains(localCurrentParent[0]))
                            .forEach(s -> allLayers.put(
                                literal(s), s));
                    }
                }
                Set<Component> ret = new LinkedHashSet<>();
                ret.add(translatable("bindlayers.gui.none"));
                ret.addAll(allLayers.keySet());
                return ret;
            },
            (c) -> {
                if (currentSelected != null) {
                    BindLayer layer = newLayers.get(currentSelected);
                    String l = allLayers.get(c);
                    if (l != null) {
                        unmaskParent(layer);
                        layer.setParentLayer(l);
                        maskParent(layer);
                        remaskChildren(layer);
                    }
                }
            },
            null
        ));

        Map<Component, String> nonConflictLayers = new LinkedHashMap<>();
        final String[] localCurrentMerge = {currentSelected};

        // `merge` dropdown
        addRenderableWidget(new DropDownWidget(
            width / 2 + 105,
            height / 2 + 50,
            95,
            12,
            () -> translatable("bindlayers.gui.none"),
            () -> {
                if (!Objects.equals(localCurrentMerge[0], currentSelected)) {
                    nonConflictLayers.clear();
                    localCurrentMerge[0] = currentSelected;
                    newLayers.keySet()
                        .stream()
                        .filter(s -> !s.equals(BindLayers.INSTANCE.vanillaLayer.name) && !conflicts.computeIfAbsent(
                            newLayers.get(currentSelected),
                            c -> new HashSet<>()
                        ).contains(newLayers.get(currentSelected)) && !s.equals(currentSelected))
                        .forEach(s -> nonConflictLayers.put(
                            literal(s), s));
                }
                Set<Component> ret = new LinkedHashSet<>();
                ret.add(translatable("bindlayers.gui.none"));
                ret.addAll(nonConflictLayers.keySet());
                return ret;
            },
            (c) -> {
                if (currentSelected != null) {
                    BindLayer layer = newLayers.get(currentSelected);
                    String ln = nonConflictLayers.get(c);
                    if (ln == null) {
                        return;
                    }
                    BindLayer merge = newLayers.get(ln);
                    merge(layer, merge);
                    localCurrentMerge[0] = null;
                }
            },
            null
        ));

        // force-merge dropdown
        addRenderableWidget(new DropDownWidget(
            width / 2 + 5,
            height / 2 + 75,
            95,
            12,
            () -> translatable("bindlayers.gui.none"),
            () -> {
                if (!Objects.equals(localCurrentMerge[0], currentSelected)) {
                    nonConflictLayers.clear();
                    localCurrentMerge[0] = currentSelected;
                    newLayers.keySet()
                        .stream()
                        .filter(s -> !s.equals(BindLayers.INSTANCE.vanillaLayer.name) && !s.equals(currentSelected))
                        .forEach(s -> nonConflictLayers.put(
                            literal(s), s));
                }
                Set<Component> ret = new LinkedHashSet<>();
                ret.add(translatable("bindlayers.gui.none"));
                ret.addAll(nonConflictLayers.keySet());
                return ret;
            },
            (c) -> {
                if (currentSelected != null) {
                    BindLayer layer = newLayers.get(currentSelected);
                    String ln = nonConflictLayers.get(c);
                    if (ln == null) {
                        return;
                    }
                    BindLayer merge = newLayers.get(ln);
                    merge(layer, merge);
                    localCurrentMerge[0] = null;
                }
            },
            null
        ));


        // cancel
        cancel = addRenderableWidget(new Button(
            width / 2 - 100,
            height - 30,
            100,
            20,
            translatable("gui.cancel"),
            (button) -> onClose()
        ));

        // save
        save = addRenderableWidget(new Button(
            width / 2,
            height - 30,
            100,
            20,
            translatable("gui.done"),
            (button) -> {
                INSTANCE.vanillaLayer.copyFrom(newLayers.get(INSTANCE.vanillaLayer.name));
                for (BindLayer layer : newLayers.values()) {
                    BindLayer l = INSTANCE.getOrCreate(layer.name);
                    l.copyFrom(layer);
                }
                for (String layer : INSTANCE.availableLayers()) {
                    if (!newLayers.containsKey(layer)) {
                        BindLayer l = INSTANCE.removeLayer(layer);
                        l.removeFile();
                    }
                }
                assert minecraft != null;
                minecraft.options.save();
                onClose();
            }
        ));

        updateSelected();
    }

    private void merge(BindLayer layer, BindLayer merge) {
        unmaskParent(layer);
        unmaskParent(merge);
        layer.addAll(merge);
        maskParent(layer);
        newLayers.remove(merge.name);
        conflicts.computeIfAbsent(layer, cs -> new HashSet<>()).addAll(conflicts.getOrDefault(merge, new HashSet<>()));
        conflicts.remove(merge);
        for (Set<BindLayer> set : conflicts.values()) {
            if (set.contains(merge)) {
                set.remove(merge);
                set.add(layer);
            }
        }
        for (BindLayer l : newLayers.values()) {
            if (l.getParentLayer().equals(merge.name)) {
                l.setParentLayer(layer.name);
            }
        }
        remaskChildren(layer);
        layerList.init(newLayers.keySet(), false);
        layerList.setSelected(layer.name);
    }

}
