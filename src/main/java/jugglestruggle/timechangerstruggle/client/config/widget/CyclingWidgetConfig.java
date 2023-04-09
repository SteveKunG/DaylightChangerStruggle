package jugglestruggle.timechangerstruggle.client.config.widget;

import jugglestruggle.timechangerstruggle.client.widget.SelfWidgetRender;
import jugglestruggle.timechangerstruggle.client.widget.SelfWidgetRendererInheritor;
import jugglestruggle.timechangerstruggle.config.property.BaseProperty;
import jugglestruggle.timechangerstruggle.config.property.BooleanValue;
import jugglestruggle.timechangerstruggle.config.property.EnumValue;
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
 * @author JuggleStruggle
 * @implNote Created on 30-Jan-2022, Sunday
 */
@Environment(EnvType.CLIENT)
public class CyclingWidgetConfig<B extends BaseProperty<B, T>, T> extends CycleButton<T> 
implements WidgetConfigInterface<B, T>, SelfWidgetRendererInheritor<CyclingWidgetConfig<B, T>>
{
	private final B property;
	private final SelfWidgetRender<CyclingWidgetConfig<B, T>> renderer;
	private T initial;
	/**
	 * This cycling widget config will provide its own callback as to avoid using mixins again,
	 * but that callback will also call this callback :)
	 */
	private final OnValueChange<T> externalCallback;
	
	protected CyclingWidgetConfig(B property, int width, int height, Component message, Component optionText, 
		int index, T value, ValueListSupplier<T> values, Function<T, Component> valueToText,
		Function<CycleButton<T>, MutableComponent> narrationMessageFactory, 
		OnValueChange<T> externalCallback, TooltipSupplier<T> tooltipFactory, boolean optionTextOmitted)
	{
		super(0, 0, width, height, message, optionText, 
			index, value, values, valueToText, 
			narrationMessageFactory, new SetPropertyValueCallback<B, T>(),
			tooltipFactory, optionTextOmitted);
		
		this.property = property;
		this.initial = property.get();
		
		this.externalCallback = externalCallback;
		this.renderer = new SelfWidgetRender<>(this, null);
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public B getProperty() {
		return this.property;
	}
	@Override
	public T getInitialValue() {
		return this.initial;
	}
	@Override
	public void setInitialValue(T value) {
		this.initial = value;
	}
	@Override
	public boolean isDefaultValue() {
		return false;
	}
	@Override
	public void setValue(T value)
	{
		super.setValue(value);
		this.onValueChanged(value);
	}
	private void onValueChanged(T newValue)
	{
		this.property.set(newValue);
		
		if (this.externalCallback != null) {
			this.externalCallback.onValueChange(this, newValue);
		}
	}
	@Override
	public void forceSetWidgetValueToDefault(boolean justInitial) {
		this.setPropertyValueToDefault(justInitial);
	}
	@Override
	public void setPropertyValueToDefault(boolean justInitial)
	{
		if (justInitial)
		{
			if (this.initial != null)
				this.setValue(this.initial);
		}
		else
		{
			final T defaultValue = this.property.getDefaultValue();
			
			if (defaultValue != null)
				this.setValue(defaultValue);
		}
	}
	
	
	
	
	
	
	@Override
	public SelfWidgetRender<CyclingWidgetConfig<B, T>> getWidgetRenderer() {
		return this.renderer;
	}
	@Override
	public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
		this.renderer.render(matrices, mouseX, mouseY, delta);
	}
	
	
	
	
	
	
	public static WidgetConfigBuilderBoolean booleanCycle(BooleanValue property, Component trueText, Component falseText)
	{
		Function<Boolean, Component> valueToText;
		
		final boolean trueTextIsNull = trueText == null;
		final boolean falseTextIsNull = falseText == null;
		
		if (trueTextIsNull && falseTextIsNull)
			valueToText = state -> Component.empty();
		else if (trueTextIsNull)
			valueToText = state -> falseText;
		else if (falseTextIsNull)
			valueToText = state -> trueText;
		else
			valueToText = state -> state ? trueText : falseText;
		
		WidgetConfigBuilderBoolean wcbb = new WidgetConfigBuilderBoolean(
			property, valueToText
		);
		
		wcbb.withValues(ImmutableList.of(true, false));
		wcbb.withInitialValue(property.get());
		
		return wcbb;
	}
	public static <EV extends Enum<EV>> WidgetConfigBuilderEnum<EV> enumCycle(EnumValue<EV> property)
	{
		Function<EV, Component> valueToText = property.getVTT();
		
		if (valueToText == null)
			valueToText = value -> Component.nullToEmpty(value.toString());
		
		WidgetConfigBuilderEnum<EV> wcbe = new WidgetConfigBuilderEnum<>(property, valueToText);
		
		wcbe.withValues(property.getEnumValues());
		wcbe.withInitialValue(property.get());
		
		return wcbe;
	}
	
	
	
	
	

	protected static class SetPropertyValueCallback<B extends BaseProperty<B, T>, T> implements OnValueChange<T>
	{
		protected SetPropertyValueCallback() { }
		
		@Override
		@SuppressWarnings({ "unchecked" })
		public void onValueChange(CycleButton<T> button, T value) {
			((CyclingWidgetConfig<B, T>)button).onValueChanged(value);
		}
	}
	public static class WidgetConfigBuilder<B extends BaseProperty<B, V>, V> extends CycleButton.Builder<V>
	{
		public final B propertyRepresented;
		
		public WidgetConfigBuilder(B property, Function<V, Component> valueToText) {
			super(valueToText); this.propertyRepresented = property;
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
		
		public CyclingWidgetConfig<B, V> build(int width, int height, Component optionText) {
			return this.build(width, height, optionText, (b, v) -> {});
		}
		public CyclingWidgetConfig<B, V> build(int width, int height, Component optionText, OnValueChange<V> callback)
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
			
			return new CyclingWidgetConfig<>(this.propertyRepresented, width, height, messageText, optionText, 
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
	public static class WidgetConfigBuilderBoolean extends WidgetConfigBuilder<BooleanValue, Boolean>
	{
		public WidgetConfigBuilderBoolean(BooleanValue property, Function<Boolean, Component> valueToText) {
			super(property, valueToText);
		}
	}
	public static class WidgetConfigBuilderEnum<EV extends Enum<EV>> extends WidgetConfigBuilder<EnumValue<EV>, EV>
	{
		public WidgetConfigBuilderEnum(EnumValue<EV> property, Function<EV, Component> valueToText) {
			super(property, valueToText);
		}
	}
}

