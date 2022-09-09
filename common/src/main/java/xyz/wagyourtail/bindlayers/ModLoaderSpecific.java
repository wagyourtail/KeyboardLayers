package xyz.wagyourtail.bindlayers;

import net.minecraft.client.KeyMapping;

import java.nio.file.Path;
import java.util.Map;

public interface ModLoaderSpecific {
    Path getBindDir();

    void applyBinds(Map<KeyMapping, BindLayer.Bind> binds);

    BindLayer.Bind keyMappingToBind(KeyMapping keyMapping);

    BindLayer.Bind keyMappingDefaultToBind(KeyMapping keyMapping);

}
