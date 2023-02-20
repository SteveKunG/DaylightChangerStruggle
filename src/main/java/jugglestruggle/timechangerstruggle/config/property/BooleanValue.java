package jugglestruggle.timechangerstruggle.config.property;

import jugglestruggle.timechangerstruggle.client.config.property.FancySectionProperty;
import jugglestruggle.timechangerstruggle.client.config.widget.CyclingWidgetConfig;
import jugglestruggle.timechangerstruggle.client.config.widget.WidgetConfigInterface;
import jugglestruggle.timechangerstruggle.client.config.widget.CyclingWidgetConfig.WidgetConfigBuilderBoolean;
import jugglestruggle.timechangerstruggle.client.screen.TimeChangerScreen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import java.util.Locale;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;

/**
 *
 * @author JuggleStruggle
 * @implNote Created on 31-Jan-2022, Monday
 */
@Environment(EnvType.CLIENT)
public class BooleanValue extends BaseProperty<BooleanValue, Boolean>
{
	private Component trueText = CommonComponents.OPTION_ON;
	private Component falseText = CommonComponents.OPTION_OFF;
	private Component propertyText;
	
	private final CycleButton.OnValueChange<Boolean> callback = (button, value) -> { 
		if (super.consumer != null)
			super.consumer.consume(this, value);
	};
	
	public BooleanValue(String property, boolean value) {
		super(property, value);
	}

	@Override
	public void set(Boolean value) {
		super.value = (value == null) ? false : value;
	}
	
	public BooleanValue setTrueText(Component text) {
		this.trueText = text; return this;
	}
	public BooleanValue setFalseText(Component text) {
		this.falseText = text; return this;
	}
	public BooleanValue setText(Component text) {
		this.propertyText = text; return this;
	}
	
	@Override
	public WidgetConfigInterface<BooleanValue, Boolean> createConfigElement
	(TimeChangerScreen screen, FancySectionProperty owningSection)
	{
		WidgetConfigBuilderBoolean builder = 
			CyclingWidgetConfig.booleanCycle(this, this.trueText, this.falseText);
		
		Component optionText;
		
		if (this.propertyText == null)
		{
			optionText = null;
			
			if (owningSection != null)
			{
				Component sectionText = owningSection.get();
				
				if (sectionText != null && sectionText instanceof TranslatableComponent)
				{
					optionText = new TranslatableComponent(String.format("%1$s.%2$s",
						((TranslatableComponent)sectionText).getKey(), this.property().toLowerCase(Locale.ROOT)));
				}
			}
			
			if (optionText == null)
				optionText = new TextComponent(this.property());
		}
		else
			optionText = this.propertyText;
		
		return builder.build(20, 20, optionText, this.callback);
	}
	@Override
	public ArgumentType<Boolean> onCommandOptionGetArgType() {
		return BoolArgumentType.bool();
	}
	@Override
	public int onCommandOptionWithValueExecute(CommandContext<FabricClientCommandSource> ctx) {
		this.set(BoolArgumentType.getBool(ctx, "value")); return 3;
	}
	@Override
	public boolean onCommandOptionNoValueShouldBeExecuted() {
		return true;
	}
	@Override
	public int onCommandOptionNoValueExecute(CommandContext<FabricClientCommandSource> ctx) {
		this.set(!this.get()); return 3;
	}
	
	

	@Override
	public void readFromJson(JsonElement elem) 
	{
		if (elem.isJsonPrimitive()) 
		{
			JsonPrimitive prim = elem.getAsJsonPrimitive();
			
			if (prim.isBoolean())
				this.set(prim.getAsBoolean());
		}
	}

	@Override
	public JsonElement writeToJson() {
		return new JsonPrimitive(this.get());
	}
}
