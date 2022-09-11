package xyz.wagyourtail.bindlayers.legacy;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

public class ComponentHelper {

    public static TranslatableComponent translatable(String s) {
        return new TranslatableComponent(s);
    }

    public static TextComponent literal(String s) {
        return new TextComponent(s);
    }
}
