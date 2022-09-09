package xyz.wagyourtail.bindlayers;

import de.siphalor.amecs.api.AmecsKeyBinding;
import de.siphalor.amecs.api.KeyModifier;
import de.siphalor.amecs.api.KeyModifiers;
import de.siphalor.amecs.impl.duck.IKeyBinding;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import xyz.wagyourtail.bindlayers.mixin.KeyMappingAccessor;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FabricModLoader implements ModLoaderSpecific {
    @Override
    public Path getBindDir() {
        return FabricLoader.getInstance().getGameDir().resolve("binds");
    }

    @Override
    public void applyBinds(Map<KeyMapping, BindLayer.Bind> binds) {
        if (FabricLoader.getInstance().isModLoaded("amecsapi")) {
            for (Map.Entry<KeyMapping, BindLayer.Bind> mapping : binds.entrySet()) {
                KeyModifiers modifiers = ((IKeyBinding) mapping.getKey()).amecs$getKeyModifiers();

                modifiers.unset();
                Set<KeyModifier> modifierSet = new HashSet<>();
                int mods = mapping.getValue().mods;
                for (BindLayer.Mods mod : BindLayer.Mods.values()) {
                    if ((mods & mod.code) != 0) {
                        modifierSet.add(KeyModifier.valueOf(mod.name()));
                    }
                }

                for (KeyModifier mod : modifierSet) {
                    modifiers.set(mod, true);
                }

                mapping.getKey().setKey(mapping.getValue().key);
            }
        } else {
            for (Map.Entry<KeyMapping, BindLayer.Bind> mapping : binds.entrySet()) {
                mapping.getKey().setKey(mapping.getValue().key);
            }
        }
    }

    @Override
    public BindLayer.Bind keyMappingToBind(KeyMapping keyMapping) {
        if (FabricLoader.getInstance().isModLoaded("amecsapi")) {
            KeyModifiers modifiers = ((IKeyBinding) keyMapping).amecs$getKeyModifiers();
            int mods = 0;
            for (KeyModifier mod : KeyModifier.values()) {
                if (modifiers.get(mod)) {
                    mods |= BindLayer.Mods.valueOf(mod.name()).code;
                }
            }
            return new BindLayer.Bind(((KeyMappingAccessor) keyMapping).getKey(), mods);
        } else {
            return new BindLayer.Bind(((KeyMappingAccessor) keyMapping).getKey(), 0);
        }
    }

    @Override
    public BindLayer.Bind keyMappingDefaultToBind(KeyMapping keyMapping) {
        if (FabricLoader.getInstance().isModLoaded("amecsapi")) {
            KeyModifiers modifiers =
                keyMapping instanceof AmecsKeyBinding ? AmecAccessor.getDefaultKeyMods((AmecsKeyBinding) keyMapping) : KeyModifiers.NO_MODIFIERS;
            int mods = 0;
            for (KeyModifier mod : KeyModifier.values()) {
                if (modifiers.get(mod)) {
                    mods |= BindLayer.Mods.valueOf(mod.name()).code;
                }
            }
            return new BindLayer.Bind(keyMapping.getDefaultKey(), mods);
        } else {
            return new BindLayer.Bind(keyMapping.getDefaultKey(), 0);
        }
    }

}
