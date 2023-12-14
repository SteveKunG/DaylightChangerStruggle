package jugglestruggle.timechangerstruggle.client.config.widget;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import jugglestruggle.timechangerstruggle.client.widget.PositionedTooltip;
import jugglestruggle.timechangerstruggle.config.property.BaseNumber;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 *
 * @author JuggleStruggle
 * @implNote Created on 30-Jan-2022, Sunday
 */
public class NumericFieldWidgetConfig<N extends Number> extends EditBox implements WidgetConfigInterface<BaseNumber<N>, N>, PositionedTooltip
{
    protected final BaseNumber<N> property;
    protected N initialNumber;

    private int tooltipWidth;
    private int tooltipHeight;
    private List<FormattedCharSequence> compiledTooltipText;

    protected Consumer<String> textChangedListener;

    private boolean isNewTextValid;

    public NumericFieldWidgetConfig(Font textRenderer, int width, int height, BaseNumber<N> property)
    {
        super(textRenderer, 18, 18, width, height, Component.literal(property.get().toString()));

        this.property = property;
        this.isNewTextValid = true;

        this.setResponder(null);
        this.setFilter(null);

        this.setValue(this.property.get().toString());
        this.initialNumber = this.property.get();

        this.moveCursorToStart(false);
    }

    @Override
    public void setResponder(Consumer<String> changedListener)
    {
        this.textChangedListener = changedListener;
        super.setResponder(this::onTextChanged);
    }

    /**
     * Note: Numeric Field Widget snatches the setTextPredicate as
     * the class itself only uses numbers as predicate.
     *
     * @param textPredicate useless if provided; this method is used
     * to create the predicates for an specific number type and to
     * avoid problems.
     */
    @Override
    public void setFilter(Predicate<String> textPredicate)
    {
        Predicate<String> theNextPredicate = text ->
        {
            if (!text.isBlank())
            {
                return NumericFieldWidgetConfig.canParseString(this.property.getDefaultValue(), text);
            }

            return true;
        };

        super.setFilter(theNextPredicate);
    }

    @Override
    public BaseNumber<N> getProperty()
    {
        return this.property;
    }

    @Override
    public boolean isValid()
    {
        if (!this.isNewTextValid || this.property.get() == null)
        {
            return false;
        }

        if (this.property.getMin() == null || this.property.getMax() == null)
        {
            return true;
        }

        return this.property.isWithinRange();
    }

    @Override
    public N getInitialValue()
    {
        return this.initialNumber;
    }

    @Override
    public void setInitialValue(N value)
    {
        this.initialNumber = value;
    }

    @Override
    public void forceSetWidgetValueToDefault(boolean justInitial)
    {
        if (justInitial)
        {
            this.setValue("" + (this.initialNumber == null ? "0" : this.initialNumber));
        }
        else
        {
            final var defaultNumber = this.property.getDefaultValue();
            this.setValue("" + (defaultNumber == null ? "0" : defaultNumber));
        }
    }

    @Override
    public void setPropertyValueToDefault(boolean justInitial)
    {
        if (justInitial)
        {
            this.property.set(this.initialNumber == null ? this.getZero() : this.initialNumber);
        }
        else
        {
            final var defaultNumber = this.property.getDefaultValue();
            this.property.set(defaultNumber == null ? this.getZero() : defaultNumber);
        }
    }

    @Override
    public boolean isDefaultValue()
    {
        return this.property.get().equals(this.property.getDefaultValue());
    }

    private N getZero()
    {
        return NumericFieldWidgetConfig.parseString(this.property.getDefaultValue(), "0");
    }

    private void onTextChanged(String newText)
    {
        var valid = !(newText.isEmpty() || newText.isBlank());

        if (valid)
        {
            var parsedNumber = NumericFieldWidgetConfig.parseString(this.property.getDefaultValue(), newText);

            if (parsedNumber == null)
            {
                valid = false;
            }
            else
            {
                var tempNewTextValid = this.isNewTextValid;
                var previousNumber = this.property.get();

                // to avoid the numbers from not being valid despite them being it
                this.isNewTextValid = true;
                this.property.set(parsedNumber);

                valid = this.isValid();

                // just set it back after we are done :)
                this.property.set(previousNumber);
                this.isNewTextValid = tempNewTextValid;
            }

            if (valid)
            {
                var consumer = this.property.getConsumer();

                if (consumer != null)
                {
                    consumer.consume(this.property, parsedNumber);
                }

                this.property.set(parsedNumber);
            }
        }

        this.isNewTextValid = valid;
        this.setTextColor(valid ? DEFAULT_TEXT_COLOR : 0xE06060);

        if (this.textChangedListener != null)
        {
            this.textChangedListener.accept(newText);
        }
    }

    @Override
    public int getTooltipWidth()
    {
        return this.tooltipWidth;
    }

    @Override
    public int getTooltipHeight()
    {
        return this.tooltipHeight;
    }

    @Override
    public void setTooltipWidth(int width)
    {
        this.tooltipWidth = width;
    }

    @Override
    public void setTooltipHeight(int height)
    {
        this.tooltipHeight = height;
    }

    @Override
    public List<FormattedCharSequence> getOrderedTooltip()
    {
        return this.compiledTooltipText;
    }

    @Override
    public void setOrderedTooltip(List<FormattedCharSequence> textToSet)
    {
        this.compiledTooltipText = textToSet;
    }

    protected static final boolean canParseString(Number n, String val)
    {
        return NumericFieldWidgetConfig.parseString(n, val) != null;
    }

    @SuppressWarnings("unchecked")
    protected static final <N> N parseString(N n, String val)
    {
        if (n == null || val == null)
        {
            return null;
        }

        try
        {
            if (n instanceof Integer)
            {
                return (N)(Integer)Integer.parseInt(val);
            }
            else if (n instanceof Long)
            {
                return (N)(Long)Long.parseLong(val);
            }
            else if (n instanceof Double)
            {
                return (N)(Double)Double.parseDouble(val);
            }
            else if (n instanceof Float)
            {
                return (N)(Float)Float.parseFloat(val);
            }
            else if (n instanceof Byte)
            {
                return (N)(Byte)Byte.parseByte(val);
            }
        }
        catch (NumberFormatException nfe)
        {
        }

        return null;
    }
}