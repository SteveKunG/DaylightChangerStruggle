package jugglestruggle.timechangerstruggle.client.widget;

import jugglestruggle.timechangerstruggle.client.config.widget.CyclingWidgetConfig;
import jugglestruggle.timechangerstruggle.mixin.client.widget.CyclingButtonWidgetBuilderAccessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.OptionInstance.TooltipSupplier;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.List;
import java.util.function.Function;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * See {@link CyclingWidgetConfig}'s description.
 * 
 * <p> This is a copy of CyclingWidgetConfig which has removed certain
 * config/property-related settings for general use
 *
 * @author JuggleStruggle
 * @implNote Created on 13-Feb-2022, Sunday
 */
@Environment(EnvType.CLIENT)
public class CyclingButtonWidgetEx<T> extends CycleButton<T> 
implements SelfWidgetRendererInheritor<CyclingButtonWidgetEx<T>>
{
	private final SelfWidgetRender<CyclingButtonWidgetEx<T>> renderer;
//	private T initial;
	
	protected CyclingButtonWidgetEx(int width, int height, Component message, Component optionText, 
		int index, T value, ValueListSupplier<T> values, Function<T, Component> valueToText,
		Function<CycleButton<T>, MutableComponent> narrationMessageFactory, 
		OnValueChange<T> callback, TooltipSupplier<T> tooltipFactory, boolean optionTextOmitted)
	{
		super(0, 0, width, height, message, optionText, 
			index, value, values, valueToText, 
			narrationMessageFactory, callback,
			tooltipFactory, optionTextOmitted);
		
//		this.initial = value;
		this.renderer = new SelfWidgetRender<>(this, null);
	}

	
	
	
	@Override
	public SelfWidgetRender<CyclingButtonWidgetEx<T>> getWidgetRenderer() {
		return this.renderer;
	}
	@Override
	public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
		this.renderer.render(matrices, mouseX, mouseY, delta);
	}
	
	
	
	
	
	
	public static WidgetBuilder<Boolean> booleanCycle(boolean initial, Component trueText, Component falseText)
	{
		Function<Boolean, Component> valueToText;
		
		final boolean trueTextIsNull = trueText == null;
		final boolean falseTextIsNull = falseText == null;
		
		if (trueTextIsNull && falseTextIsNull)
			valueToText = state -> { return Component.empty(); };
		else if (trueTextIsNull)
			valueToText = state -> { return falseText; };
		else if (falseTextIsNull)
			valueToText = state -> { return trueText; };
		else
			valueToText = state -> { return state ? trueText : falseText; };
		
		WidgetBuilder<Boolean> wcbb = new WidgetBuilder<>(valueToText);
		
		wcbb.withValues(ImmutableList.of(true, false));
		wcbb.withInitialValue(initial);
		
		return wcbb;
	}
	
	
	
	
	

	public static class WidgetBuilder<V> extends CycleButton.Builder<V>
	{
		public WidgetBuilder(Function<V, Component> valueToText) {
			super(valueToText); 
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public Builder<V> withInitialValue(V value)
		{
			final CyclingButtonWidgetBuilderAccessor<V> accessor = 
				(CyclingButtonWidgetBuilderAccessor<V>)this;
			
			accessor.setValue(value);
			
			int valueIndex = accessor.values().getDefaultList().indexOf(value);
			
			// means that it doesn't exist
			if (valueIndex != -1)
				accessor.setInitialIndex(valueIndex);
				
			return this;
		}
		
		public CyclingButtonWidgetEx<V> build(int width, int height, Component optionText) {
			return this.build(width, height, optionText, (b, v) -> {});
		}
		public CyclingButtonWidgetEx<V> build(int width, int height, Component optionText, OnValueChange<V> callback)
		{
			@SuppressWarnings("unchecked")
			final CyclingButtonWidgetBuilderAccessor<V> accessor = 
			(CyclingButtonWidgetBuilderAccessor<V>)this;
			
			List<V> defaults = accessor.values().getDefaultList();
			
			V startingValue = accessor.getValue();
			startingValue = startingValue == null ? defaults.get(accessor.getInitialIndex()) : startingValue;
			
			Component messageText = accessor.getValueToText().apply(startingValue);
			
			if (!accessor.omitOptionText())
				messageText = CommonComponents.optionNameValue(optionText, messageText);
			
			return new CyclingButtonWidgetEx<>(width, height, messageText, optionText, 
				accessor.getInitialIndex(), startingValue, accessor.values(), accessor.getValueToText(), 
				accessor.getNarrationMessageFactory(), callback, accessor.getTooltipFactory(), accessor.omitOptionText());
		}
		
		@Override
		@Deprecated
		public CycleButton<V> create(int x, int y, int width, int height, Component optionText) {
			return null;
		}
		@Override
		@Deprecated
		public CycleButton<V> create(int x, int y, int width, int height, Component optionText, OnValueChange<V> callback) {
			return null;
		}
	}
}

