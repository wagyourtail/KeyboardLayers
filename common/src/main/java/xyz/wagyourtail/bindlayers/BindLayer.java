package xyz.wagyourtail.bindlayers;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.jetbrains.annotations.NotNull;
import xyz.wagyourtail.bindlayers.mixin.KeyMappingAccessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public class BindLayer {
    public static final Minecraft mc = Minecraft.getInstance();
    public final String name;
    private String parentLayer;
    public final Path file;

    public final Map<KeyMapping, InputConstants.Key> binds = new HashMap<>();

    private BindLayer(String name) {
        this.name = name;
        this.file = BindLayers.bindDir.resolve(name + ".txt");
    }

    public BindLayer(String name, String parentLayer) {
        this(name);
        this.parentLayer = parentLayer;
    }

    public String getParentLayer() {
        if (parentLayer == null) {
            parentLayer = BindLayers.INSTANCE.defaultLayer.name;
        }
        return parentLayer;
    }

    public void setParentLayer(@NotNull String parentLayer) {
        this.parentLayer = parentLayer;
    }

    public void load(Options options) {
        if (!Files.exists(file)) {
            return;
        }

        Map<String, KeyMapping> keyMap = new HashMap<>();
        for (KeyMapping keyMapping : options.keyMappings) {
            keyMap.put(keyMapping.getName(), keyMapping);
        }

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line = reader.readLine();
            parentLayer = line;
            while ((line = reader.readLine()) != null) {
                String[] split = line.split(":", 2);
                KeyMapping key = keyMap.get(split[0]);
                if (key != null) {
                    binds.put(key, InputConstants.getKey(split[1]));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void save() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (Map.Entry<KeyMapping, InputConstants.Key> mapping : binds.entrySet()) {
                writer.write(parentLayer);
                writer.write("\n");
                writer.write(mapping.getKey().getName() + ":" + mapping.getValue().getName());
                writer.write("\n");
            }
        }
    }

    public void copyFrom(KeyMapping[] mappings) {
        binds.clear();
        for (KeyMapping mapping : mappings) {
            binds.put(mapping, ((KeyMappingAccessor) mapping).getKey());
        }
    }

    public void applyLayer() {
        for (Map.Entry<KeyMapping, InputConstants.Key> mapping : binds.entrySet()) {
            mapping.getKey().setKey(mapping.getValue());
        }
    }
}
