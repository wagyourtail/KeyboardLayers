package xyz.wagyourtail.bindlayers;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class BindLayers {
    protected static final Minecraft mc = Minecraft.getInstance();
    public static final ModLoaderSpecific provider = ServiceLoader.load(ModLoaderSpecific.class).iterator().next();
    static final Path bindDir = provider.getBindDir();
    public static BindLayers INSTANCE = new BindLayers();

    private final Map<String, BindLayer> layers = new HashMap<>();

    public final BindLayer defaultLayer = new BindLayer("default", "default");

    private BindLayer activeLayer = defaultLayer;
    private List<BindLayer> layerStack = ImmutableList.of(activeLayer);

    public BindLayers() {
        if (INSTANCE != null) {
            throw new IllegalStateException("BindLayers already initialized!");
        }
        INSTANCE = this;
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
            files.forEach(path -> {
                if (Files.isRegularFile(path)) {
                    String name = path.getFileName().toString();
                    BindLayer layer = new BindLayer(name.substring(0, name.lastIndexOf('.')), null);
                    layer.load(gameOptions);
                    layers.put(layer.name, layer);
                }
            });
        }

        defaultLayer.copyFrom(gameOptions.keyMappings);

        setActiveLayer(active);
    }

    public BindLayer getOrCreate(String name) {
        return layers.computeIfAbsent(name, (s) -> new BindLayer(s, defaultLayer.name));
    }

    public Set<String> availableLayers() {
        return layers.keySet();
    }


    public void onTick() {
        //todo: next layer button
    }

    public String getActiveLayer() {
        return activeLayer.name;
    }

    public void setActiveLayer(String name) {
        LinkedHashSet<BindLayer> layers = new LinkedHashSet<>();
        BindLayer layer = activeLayer = getOrCreate(name);
        while (layer != defaultLayer) {
            if (!layers.add(layer)) {
                System.err.println("Layer loop detected!, fixing by stopping now at default layer");
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
    }

    public List<BindLayer> getLayerStack() {
        return layerStack;
    }

    public static <E> E lastIn(Iterable<E> set) {
        Iterator<E> iterator = set.iterator();
        E last = null;
        while (iterator.hasNext()) {
            last = iterator.next();
        }
        return last;
    }
}
