package jugglestruggle.timechangerstruggle.mixin.client.widget;

import java.util.function.Function;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.CycleButton.ValueListSupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 *
 * @author JuggleStruggle
 * @implNote Created on 03-Feb-2022, Thursday
 */
@Mixin(CycleButton.Builder.class)
public interface CyclingButtonWidgetBuilderAccessor<T>
{
	@Accessor("value")
	T getValue();
	@Accessor("value")
	void setValue(T value);
	
	@Accessor("values")
	ValueListSupplier<T> values();
	
	@Accessor("initialIndex")
	int getInitialIndex();
	@Accessor("initialIndex")
	void setInitialIndex(int initialIndex);

	@Accessor("valueToText")
	Function<T, Component> getValueToText();

	@Accessor("optionTextOmitted")
	boolean omitOptionText();

	@Accessor("narrationMessageFactory")
	Function<CycleButton<T>, MutableComponent> getNarrationMessageFactory();

	@Accessor("tooltipFactory")
	OptionInstance.TooltipSupplier<T> getTooltipFactory();
}
