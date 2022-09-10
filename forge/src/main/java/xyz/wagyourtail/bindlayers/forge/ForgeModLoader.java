package xyz.wagyourtail.bindlayers.forge;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.extensions.IForgeKeyMapping;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.loading.FMLLoader;
import xyz.wagyourtail.bindlayers.BindLayer;
import xyz.wagyourtail.bindlayers.ModLoaderSpecific;

import java.util.Map;

public class ForgeModLoader implements ModLoaderSpecific {
    @Override
    public java.nio.file.Path getBindDir() {
        return FMLLoader.getGamePath().resolve("binds");
    }

    @Override
    public void applyBinds(Map<KeyMapping, BindLayer.Bind> binds) {
        for (Map.Entry<KeyMapping, BindLayer.Bind> mapping : binds.entrySet()) {
            mapping.getKey().setKey(mapping.getValue().key);
            KeyModifier mod = KeyModifier.NONE;
            for (BindLayer.Mods mods : BindLayer.Mods.values()) {
                if (mods.code == mapping.getValue().mods) {
                    mod = KeyModifier.valueOf(mods.name());
                }
            }
            ((IForgeKeyMapping) mapping.getKey()).setKeyModifierAndCode(mod, mapping.getValue().key);
        }
    }

    @Override
    public BindLayer.Bind keyMappingToBind(KeyMapping keyMapping) {
        KeyModifier mod = ((IForgeKeyMapping) keyMapping).getKeyModifier();
        return new BindLayer.Bind(
            keyMapping.getKey(),
            mod == KeyModifier.NONE ? 0 : BindLayer.Mods.valueOf(mod.name()).code
        );
    }

    @Override
    public BindLayer.Bind keyMappingDefaultToBind(KeyMapping keyMapping) {
        KeyModifier mod = ((IForgeKeyMapping) keyMapping).getDefaultKeyModifier();
        return new BindLayer.Bind(
            keyMapping.getDefaultKey(),
            mod == KeyModifier.NONE ? 0 : BindLayer.Mods.valueOf(mod.name()).code
        );
    }

}
