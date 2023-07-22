package jugglestruggle.timechangerstruggle.client.config.widget;

import java.util.List;
import java.util.function.Consumer;

import jugglestruggle.timechangerstruggle.client.widget.PositionedTooltip;
import jugglestruggle.timechangerstruggle.config.property.BaseProperty.ValueConsumer;
import jugglestruggle.timechangerstruggle.config.property.StringValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 *
 * @author JuggleStruggle
 * @implNote Created on 30-Jan-2022, Sunday
 */
@Environment(EnvType.CLIENT)
public class TextFieldWidgetConfig extends EditBox 
implements WidgetConfigInterface<StringValue, String>, PositionedTooltip
{
    String initialText;
    final StringValue property;
    protected boolean allowEmptyText;

    private int tooltipWidth;
    private int tooltipHeight;
    private List<FormattedCharSequence> compiledTooltipText;
    private Consumer<String> textChangedListener;
    
    public TextFieldWidgetConfig(Font textRenderer, int width, int height, 
        StringValue property, boolean allowEmptyText) 
    {
        super(textRenderer, 0, 0, width, height, Component.empty());
        
        this.property = property;
        this.allowEmptyText = allowEmptyText;
        this.initialText = property.get();
        
        this.setValue(this.initialText);
        this.setResponder(null);
        
        this.moveCursorToStart();
    }

    @Override
    public boolean isValid() {
        return this.allowEmptyText ? true : !this.getValue().isBlank();
    }
    
    @Override
    public StringValue getProperty() {
        return this.property;
    }
    @Override
    public String getInitialValue() {
        return this.initialText;
    }
    @Override
    public void setInitialValue(String value) {
        this.initialText = value;
    }
    @Override
    public boolean isDefaultValue() {
        return this.property.getDefaultValue().equals(this.property.get());
    }
    @Override
    public void forceSetWidgetValueToDefault(boolean justInitial)
    {
        if (justInitial) {
            super.setValue((this.initialText == null) ? "" : this.initialText);
        } 
        else 
        {
            String def = this.property.getDefaultValue();
            super.setValue((def == null) ? "" : def);
        }
    }
    @Override
    public void setPropertyValueToDefault(boolean justInitial)
    {
        if (justInitial) {
            this.property.set((this.initialText == null) ? "" : this.initialText);
        } 
        else 
        {
            String def = this.property.getDefaultValue();
            this.property.set((def == null) ? "" : def);
        }
    }

    @Override
    public void setResponder(Consumer<String> changedListener)
    {
        this.textChangedListener = changedListener;
        super.setResponder(this::onTextChanged);
    }
    private void onTextChanged(String newText)
    {
        ValueConsumer<StringValue, String> consumer = this.property.getConsumer();
        
        if (consumer != null) {
            consumer.consume(this.property, newText);
        }
        
        this.property.set(newText);
        
        if (this.textChangedListener != null) {
            this.textChangedListener.accept(newText);
        }
    }
    
    
    
    @Override
    public int getTooltipWidth() {
        return this.tooltipWidth;
    }
    @Override
    public int getTooltipHeight() {
        return this.tooltipHeight;
    }
    @Override
    public void setTooltipWidth(int width) {
        this.tooltipWidth = width;
    }
    @Override
    public void setTooltipHeight(int height) {
        this.tooltipHeight = height;
    }
    
//    @Override
//    public List<FormattedCharSequence> getTooltip() {
//        return this.compiledTooltipText;
//    }
    @Override
    public void setOrderedTooltip(List<FormattedCharSequence> textToSet) {
        this.compiledTooltipText = textToSet;
    }
}
