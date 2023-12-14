package jugglestruggle.timechangerstruggle.client.widget;

import net.minecraft.client.gui.components.AbstractWidget;

/**
 *
 * @author JuggleStruggle
 * @implNote Created on 13-Feb-2022, Sunday
 */
public interface SelfWidgetRendererInheritor<W extends AbstractWidget>
{
    SelfWidgetRender<W> getWidgetRenderer();
}
