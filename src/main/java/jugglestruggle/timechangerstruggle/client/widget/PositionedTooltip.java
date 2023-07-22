package jugglestruggle.timechangerstruggle.client.widget;

import java.util.List;

import com.google.common.collect.ImmutableList;

import jugglestruggle.timechangerstruggle.client.screen.TimeChangerScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * 
 *
 * @author JuggleStruggle
 * @implNote Created on 06-Feb-2022, Sunday
 */
@Environment(EnvType.CLIENT)
//TODO
public interface PositionedTooltip/* extends TooltipAccessor*/
{
    int getTooltipWidth();
    int getTooltipHeight();
    
    void setTooltipWidth(int width);
    void setTooltipHeight(int height);
    
    void setOrderedTooltip(List<FormattedCharSequence> textToSet);
    
    default void updateTooltip(Component tooltipDescText, Component tooltipText, Font renderer)
    {
        final boolean descIsNull = tooltipDescText == null;
        final boolean tooltipIsNull = tooltipText == null;
        
        List<FormattedCharSequence> compiledTooltipText;
        
        if (descIsNull && tooltipIsNull) {
            compiledTooltipText = ImmutableList.of();
        } 
        else
        {
            byte useCase;
            
            if (descIsNull)
                useCase = 1;
            else if (tooltipIsNull)
                useCase = 3;
            else
                useCase = 2;
            
            
            compiledTooltipText = TimeChangerScreen.createOrderedTooltips(
                renderer, useCase, tooltipDescText, tooltipText
            );
        }
        
        final int[] offsetPos = TimeChangerScreen.getTooltipForWidgetWidthHeight(compiledTooltipText, renderer);
        this.setTooltipWidth(offsetPos[0]); this.setTooltipHeight(offsetPos[1]); 
        
        this.setOrderedTooltip(compiledTooltipText);
    }
}
