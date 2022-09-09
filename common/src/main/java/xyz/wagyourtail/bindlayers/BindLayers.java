package xyz.wagyourtail.bindlayers;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.wagyourtail.bindlayers.screen.QuickSelectScreen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class BindLayers {
    protected static final Minecraft mc = Minecraft.getInstance();
    protected static final Logger LOGGER = LoggerFactory.getLogger(BindLayers.class);
    public static final ModLoaderSpecific provider = ServiceLoader.load(ModLoaderSpecific.class).iterator().next();
    static final Path bindDir = provider.getBindDir();
    public static BindLayers INSTANCE = new BindLayers();

    private final Map<String, BindLayer> layers = new Object2ObjectRBTreeMap<>();

    public final BindLayer defaultLayer = new BindLayer("default", "default");

    public final KeyMapping nextLayer = new KeyMapping(
        "bindlayers.next_layer",
        InputConstants.KEY_LBRACKET,
        "bindlayers.category"
    );
    public final KeyMapping prevLayer = new KeyMapping(
        "bindlayers.prev_layer",
        InputConstants.KEY_RBRACKET,
        "bindlayers.category"
    );
    public final KeyMapping quickSelect = new KeyMapping(
        "bindlayers.quick_select",
        InputConstants.KEY_BACKSLASH,
        "bindlayers.category"
    );

    private BindLayer activeLayer = defaultLayer;
    private List<BindLayer> layerStack = ImmutableList.of(activeLayer);

    private LayerToast toast = null;

    public BindLayers() {
        if (INSTANCE != null) {
            throw new IllegalStateException("BindLayers already initialized!");
        }
        INSTANCE = this;
    }

    public static <E> E lastIn(Iterable<E> set) {
        Iterator<E> iterator = set.iterator();
        E last = null;
        while (iterator.hasNext()) {
            last = iterator.next();
        }
        return last;
    }

    public void onGameOptionsLoad(Options gameOptions) throws IOException {
        layers.clear();
        layers.put("default", defaultLayer);
        String active = activeLayer.name;

        if (!Files.exists(bindDir)) {
            try {
                Files.createDirectory(bindDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try (Stream<Path> files = Files.list(bindDir)) {
            LOGGER.info("Loading BindLayers...");
            files.forEach(path -> {
                if (Files.isRegularFile(path)) {
                    String name = path.getFileName().toString();
                    BindLayer layer = new BindLayer(name.substring(0, name.lastIndexOf('.')), null);
                    layer.load(gameOptions);
                    layers.put(layer.name, layer);
                    LOGGER.info("Discovered layer: {}", layer.name);
                }
            });
        }

        defaultLayer.copyFrom(gameOptions.keyMappings);

        setActiveLayer(active);
    }

    public void onTick() {
        if (nextLayer.consumeClick()) {
            List<String> layers = new ArrayList<>(availableLayers());
            int index = layers.indexOf(activeLayer.name);
            if (index == layers.size() - 1) {
                index = 0;
            } else {
                index++;
            }
            setActiveLayer(layers.get(index));
        }
        if (prevLayer.consumeClick()) {
            List<String> layers = new ArrayList<>(availableLayers());
            int index = layers.indexOf(activeLayer.name);
            if (index == 0) {
                index = layers.size() - 1;
            } else {
                index--;
            }
            setActiveLayer(layers.get(index));
        }
        if (quickSelect.consumeClick()) {
            mc.setScreen(new QuickSelectScreen());
        }
    }

    public Set<String> availableLayers() {
        return layers.keySet();
    }

    public String getActiveLayer() {
        return activeLayer.name;
    }

    public void setActiveLayer(String name) {
        setActiveLayer(name, false);
    }

    @ApiStatus.Internal
    public void setActiveLayer(String name, boolean quiet) {
        LinkedHashSet<BindLayer> layers = new LinkedHashSet<>();
        BindLayer layer = activeLayer = getOrCreate(name);
        while (layer != defaultLayer) {
            if (!layers.add(layer)) {
                LOGGER.warn("Layer loop detected!, fixing by stopping now at default layer");
                break;
            }
            layer = getOrCreate(layer.getParentLayer());
        }
        layers.add(defaultLayer);
        List<BindLayer> layerList = new ArrayList<>(layers);
        Collections.reverse(layerList);
        for (BindLayer bindLayer : layerList) {
            bindLayer.applyLayer();
        }
        layerStack = ImmutableList.copyOf(layerList);

        KeyMapping.resetMapping();

        try {
            if (!quiet) {
                if (toast == null || toast.isDone) {
                    toast = new LayerToast(SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
                        Component.translatable("bindlayers.toast.layer_change.title"), Component.literal(name)
                    );
                    mc.getToasts().addToast(toast);
                } else {
                    toast.reset(Component.translatable("bindlayers.toast.layer_change.title"), Component.literal(name));
                }
            }
        } catch (Exception ignored) {
        }
    }

    public BindLayer getOrCreate(String name) {
        return layers.computeIfAbsent(name, (s) -> new BindLayer(s, defaultLayer.name));
    }

    public List<BindLayer> getLayerStack() {
        return layerStack;
    }

    public BindLayer removeLayer(String name) {
        return layers.remove(name);
    }

    public Set<BindLayer> getChildLayers(String name) {
        Set<BindLayer> layers = new LinkedHashSet<>();
        for (BindLayer layer : this.layers.values()) {
            if (layer.getParentLayer().equals(name)) {
                if (!layer.name.equals("default")) {
                    layers.add(layer);
                    layers.addAll(getChildLayers(layer.name));
                }
            }
        }
        return layers;
    }

}
