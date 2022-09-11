package xyz.wagyourtail.bindlayers;

import de.siphalor.amecs.api.AmecsKeyBinding;
import de.siphalor.amecs.api.KeyModifiers;
import de.siphalor.amecs.impl.duck.IKeyBinding;
import net.minecraft.client.KeyMapping;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public class AmecAccessor {

    public static final MethodHandle defaultMods;

    static {
        try {
            Field[] fields = AmecsKeyBinding.class.getDeclaredFields();
            MethodHandle dm = null;
            for (Field f : fields) {
                if (f.getType() == KeyModifiers.class) {
                    f.setAccessible(true);
                    dm = MethodHandles.lookup().unreflectGetter(f);
                }
            }
            defaultMods = dm;
            if (defaultMods == null) throw new NoSuchFieldException("defaultMods");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyModifiers getDefaultKeyMods(AmecsKeyBinding binding) {
        try {
            return (KeyModifiers) defaultMods.invoke(binding);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    public static void resetAmecsKeybind(KeyMapping keyMapping) {
        ((IKeyBinding) keyMapping).amecs$getKeyModifiers().unset();
        if (keyMapping instanceof AmecsKeyBinding) {
            ((AmecsKeyBinding) keyMapping).resetKeyBinding();
        }
    }
}
