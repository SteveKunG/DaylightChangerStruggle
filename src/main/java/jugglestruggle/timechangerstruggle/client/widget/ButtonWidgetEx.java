package jugglestruggle.timechangerstruggle.client.widget;

import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 *
 *
 * @author JuggleStruggle
 * @implNote Created on 13-Feb-2022, Sunday
 */
public class ButtonWidgetEx extends Button implements PositionedTooltip
{
    private int tooltipWidth;
    private int tooltipHeight;
    private List<FormattedCharSequence> compiledTooltipText;

    public ButtonWidgetEx(int width, int height, Component message, Component tooltipDescText, Component tooltipText, Font renderer, OnPress onPress)
    {
        super(0, 0, width, height, message, onPress, DEFAULT_NARRATION);
        this.updateTooltip(tooltipDescText, tooltipText, renderer);
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
}