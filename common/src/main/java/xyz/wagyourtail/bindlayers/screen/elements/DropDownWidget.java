package xyz.wagyourtail.bindlayers.screen.elements;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.minecraft.network.chat.Component.translatable;

public class DropDownWidget extends AbstractButton {
    final Supplier<Component> selected;
    final Supplier<Set<Component>> options;
    final Consumer<Component> select;
    @Nullable
    final Runnable addOption;

    Deque<AbstractWidget> children = new ArrayDeque<>();

    Deque<AbstractWidget> hiddenChildren = new ArrayDeque<>();

    @SuppressWarnings("ConstantConditions")
    public DropDownWidget(int i, int j, int k, int l, Supplier<Component> selected, Supplier<Set<Component>> options, Consumer<Component> select, @Nullable Runnable addOption) {
        super(i, j, k, l, null);
        this.selected = selected;
        this.options = options;
        this.select = select;
        this.addOption = addOption;
    }

    @Override
    public void mouseMoved(double d, double e) {
        for (AbstractWidget child : children) {
            child.mouseMoved(d, e);
        }
        super.mouseMoved(d, e);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f) {
        if (f > 0 && !hiddenChildren.isEmpty()) {
            AbstractWidget w = children.removeFirst();
            children.addFirst(hiddenChildren.removeLast());
            children.addFirst(w);
        } else if (f < 0 && children.size() > 1) {
            AbstractWidget w = children.removeFirst();
            hiddenChildren.addLast(children.removeFirst());
            children.addFirst(w);
        }
        Iterator<AbstractWidget> it = children.iterator();
        int i = 0;
        while (it.hasNext()) {
            AbstractWidget w = it.next();
            w.setY(getY() + (i * 12));
            i++;
        }
        return super.mouseScrolled(d, e, f);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int i, int j, float f) {
        isHovered = isMouseOver(i, j);
        if (isFocused()) {
            poseStack.pushPose();
            poseStack.translate(0, 0, 1);
            for (AbstractWidget child : children) {
                child.render(poseStack, i, j, f);
            }
            poseStack.popPose();
        } else {
            renderWidget(poseStack, i, j, f);
        }
    }

    @Override
    public void onPress() {

    }

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        boolean bl = super.mouseClicked(d, e, i);
        for (AbstractWidget child : children) {
            if (child.mouseClicked(d, e, i)) {
                return true;
            }
        }
        setFocused(bl);
        return bl;
    }

    @Override
    public boolean mouseReleased(double d, double e, int i) {
        boolean bl = super.mouseReleased(d, e, i);
        for (AbstractWidget child : children) {
            if (child.mouseReleased(d, e, i)) {
                return true;
            }
        }
        return bl;
    }

    @Override
    public boolean mouseDragged(double d, double e, int i, double f, double g) {
        for (AbstractWidget child : children) {
            if (child.mouseDragged(d, e, i, f, g)) {
                return true;
            }
        }
        return super.mouseDragged(d, e, i, f, g);
    }

    public void onFocusedChanged(boolean bl) {
        if (isFocused()) {
            children.clear();
            int index = 0;
            if (addOption != null) {
                index++;
                children.add(new Button.Builder(
                    translatable("bindlayers.gui.add_option"),
                    (btn) -> addOption.run()
                ).bounds(
                    getX(),
                    getY(),
                    width,
                    12).build());
            }
            for (Component option : options.get()) {
                children.add(new Button.Builder(option, (button) -> {
                    select.accept(option);
                    setFocused(false);
                }).bounds(getX(), getY() + index++ * 12, width, 12).build());
            }
            this.height = index * 12;
        } else {
            this.children.clear();
            this.height = 12;
        }
    }

    @Override
    public @NotNull Component getMessage() {
        return selected.get();
    }

    @Override
    public void setFocused(boolean bl) {
        if (bl ^ isFocused()) {
            super.setFocused(bl);
            onFocusedChanged(bl);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

}
