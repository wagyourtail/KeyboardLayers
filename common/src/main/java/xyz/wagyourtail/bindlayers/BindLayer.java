package xyz.wagyourtail.bindlayers;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BindLayer {
    public final String name;
    public final Path file;
    public final Map<KeyMapping, Bind> binds = new HashMap<>();
    private String parentLayer;

    public BindLayer(String name, String parentLayer) {
        this(name);
        this.parentLayer = parentLayer;
    }

    private BindLayer(String name) {
        this.name = name;
        this.file = BindLayers.bindDir.resolve(name + ".txt");
    }

    public String getParentLayer() {
        if (parentLayer == null) {
            parentLayer = BindLayers.INSTANCE.vanillaLayer.name;
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
                String[] split = line.split(":", 3);
                KeyMapping key = keyMap.get(split[0]);
                int mods = 0;
                if (split.length == 3) {
                    for (String part : split[2].split("\\+")) {
                        mods |= Mods.valueOf(part).code;
                    }
                }
                if (key != null) {
                    binds.put(key, new Bind(InputConstants.getKey(split[1]), mods));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void save() throws IOException {
        try (
            BufferedWriter writer = Files.newBufferedWriter(
                file,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
        ) {
            writer.write(parentLayer);
            writer.write("\n");
            for (Map.Entry<KeyMapping, Bind> mapping : binds.entrySet()) {
                int mods = mapping.getValue().mods;
                InputConstants.Key key = mapping.getValue().key;
                writer.write(mapping.getKey().getName() + ":" + key.getName());
                if (mods != 0) {
                    writer.write(":");
                    boolean first = true;
                    for (Mods mod : Mods.values()) {
                        if ((mods & mod.code) != 0) {
                            if (!first) {
                                writer.write("+");
                            }
                            writer.write(mod.name());
                            first = false;
                        }
                    }
                }
                writer.write("\n");
            }
        }
    }

    public void removeFile() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void copyFrom(KeyMapping[] mappings) {
        binds.clear();
        for (KeyMapping mapping : mappings) {
            binds.put(mapping, BindLayers.provider.keyMappingToBind(mapping));
        }
    }

    public void copyFromDefault(KeyMapping[] mappings) {
        binds.clear();
        for (KeyMapping mapping : mappings) {
            binds.put(mapping, BindLayers.provider.keyMappingDefaultToBind(mapping));
        }
        parentLayer = BindLayers.INSTANCE.vanillaLayer.name;
    }

    public void copyFrom(BindLayer layer) {
        binds.clear();
        binds.putAll(layer.binds);
        parentLayer = layer.parentLayer;
    }

    public void addAll(BindLayer layer) {
        binds.putAll(layer.binds);
    }

    public void applyLayer() {
        BindLayers.provider.applyBinds(binds);
    }

    public enum Mods {
        NONE(0),
        SHIFT(1),
        CONTROL(2),
        ALT(4),
        SUPER(8);

        public final int code;

        Mods(int code) {
            this.code = code;
        }
    }

    public static class Bind {
        public static final BindLayer.Bind UNKNOWN = new BindLayer.Bind(InputConstants.UNKNOWN, 0);
        public final InputConstants.Key key;
        public final int mods;


        public Bind(InputConstants.Key key, int mods) {
            this.key = key;
            this.mods = mods;
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, mods);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Bind)) {
                return false;
            }
            Bind bind = (Bind) o;
            return mods == bind.mods && Objects.equals(key, bind.key);
        }

    }

}
