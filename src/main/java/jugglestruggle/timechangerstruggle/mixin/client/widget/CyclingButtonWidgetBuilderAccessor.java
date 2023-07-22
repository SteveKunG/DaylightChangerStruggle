package jugglestruggle.timechangerstruggle.mixin.client.widget;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.CycleButton.ValueListSupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 *
 * @author JuggleStruggle
 * @implNote Created on 03-Feb-2022, Thursday
 */
@Mixin(CycleButton.Builder.class)
public interface CyclingButtonWidgetBuilderAccessor<T>
{
    @Accessor("initialValue")
    T getValue();

    @Accessor("initialValue")
    void setValue(T value);

    @Accessor("values")
    ValueListSupplier<T> values();

    @Accessor("initialIndex")
    int getInitialIndex();

    @Accessor("initialIndex")
    void setInitialIndex(int initialIndex);

    @Accessor("valueStringifier")
    Function<T, Component> getValueToText();

    @Accessor("displayOnlyValue")
    boolean omitOptionText();

    @Accessor("narrationProvider")
    Function<CycleButton<T>, MutableComponent> getNarrationMessageFactory();

    @Accessor("tooltipSupplier")
    OptionInstance.TooltipSupplier<T> getTooltipFactory();
}