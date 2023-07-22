package jugglestruggle.timechangerstruggle.client.widget;

import jugglestruggle.timechangerstruggle.client.util.color.AbstractRGB;
import jugglestruggle.timechangerstruggle.client.util.color.RainbowRGB;
import jugglestruggle.timechangerstruggle.client.util.render.RenderUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.util.FormattedCharSequence;

/**
 *
 * @author JuggleStruggle
 * @implNote Created on 13-Feb-2022, Sunday
 */
public class SelfWidgetRender<W extends AbstractWidget>
{
    private final W widget;
    private Font textRenderer;

    private AbstractRGB textColoring;
    
    private float stripeScale;
    private float rainbowSpeed;
    
    private float rainbowOffset;
    private float previousRainbowOffset;
    
    public boolean swapTextColoringWithRainbow;
//    private AbstractRGB[] hoveredColor;
    
    public SelfWidgetRender(W widget, Font textRenderer)
    {
        this.widget = widget;
        this.textRenderer = textRenderer;

        /*
         this.hoveredColor = ChromaRGB.createColors
             (0xFFFFFFFF, 0xFFFFFF00, 0xFF00FFFF, 0xFF00FFFF, 0xFFFF00FF);
         */
        this.textColoring = RainbowRGB.createColors(0xFFFFFFFF)[0];
        
        this.stripeScale = 2.0f;
        this.rainbowSpeed = 1.0f;
        this.rainbowOffset = 0.0f;
        
        // Non-functional; if you have an idea as to how we can render text
        // using the rainbow shader then we can make this useful :D
        this.swapTextColoringWithRainbow = false;
        
        /*
         ((ChromaRGB)this.hoveredColor[1]).setTicks(0);
         ((ChromaRGB)this.hoveredColor[2]).setTicks(14);
         ((ChromaRGB)this.hoveredColor[3]).setTicks(18);
         ((ChromaRGB)this.hoveredColor[4]).setTicks(28);
         
         for (int i = 1; i < 5; ++i) {
             ((ChromaRGB)this.hoveredColor[i]).setTicksForNextUpdate(42);
         }
         */
    }
    
    public void setTextRendering(Font renderer) {
        this.textRenderer = renderer;
    }
    
    public void tick()
    {
        if (this.widget.active && this.widget.isHoveredOrFocused())
        {
            this.textColoring.tick();
            
//            float maxOffset = 76.0f;  // for scale 2 without any changes ( 6 colors)
//            float maxOffset = 152.0f; // for scale 1 without any changes ( 6 colors)
            float maxOffset = 302.0f; // for scale 1 without any changes (12 colors)
            
            if (this.stripeScale != 1.0f) {
                maxOffset /= this.stripeScale;
            }
            
            if (this.rainbowOffset > maxOffset) 
            {
                this.previousRainbowOffset = 0f;
                this.rainbowOffset = this.rainbowOffset - (maxOffset + 1.0f);
            } 
            else 
            {
                this.previousRainbowOffset = this.rainbowOffset;
                this.rainbowOffset += this.rainbowSpeed;
            }
            
            /*
            for (AbstractRGB r : this.hoveredColor) {
                r.tick();
            }
             */
        }
    }
    
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta)
    {
        boolean stcwr = false;    
        int textColor;
        
        if (this.widget.active && this.widget.isHoveredOrFocused())
        {
            /*
            textColor = this.hoveredColor[0].getInterpolatedColor(delta);
            
            RenderUtils.fillPointedGradient(matrices, this.widget.x, this.widget.y, 
                this.widget.x + this.widget.getWidth(), this.widget.y + this.widget.getHeight(), this.widget.getZOffset(), 
                this.hoveredColor[1].getInterpolatedColor(delta), this.hoveredColor[2].getInterpolatedColor(delta),
                this.hoveredColor[3].getInterpolatedColor(delta), this.hoveredColor[4].getInterpolatedColor(delta));
             */
            
            if (this.swapTextColoringWithRainbow)
            {
                stcwr = true;
                
//                textColor = this.textColoring.getInterpolatedColor(delta);
                textColor = 0xFF000000;
                
                graphics.fill(this.widget.getX(), this.widget.getY(), 
                    this.widget.getX() + this.widget.getWidth(), 
                    this.widget.getY() + this.widget.getHeight(), textColor);
                
            }
            else
            {
                textColor = this.textColoring.getInterpolatedColor(delta);
                
                this.fillMyRainbow(graphics, delta, false);
            }
        }
        else
        {
                   textColor = this.widget.active ? 0xFFFFFF : 0xA0A0A0;
            int enabledColor = this.widget.active ? 0xCC888888 : 0xCC333333;
            
            graphics.fill(this.widget.getX(), this.widget.getY(), 
                this.widget.getX() + this.widget.getWidth(), this.widget.getY() + this.widget.getHeight(), enabledColor);
        }
        
        FormattedCharSequence message = this.widget.getMessage().getVisualOrderText();
        int messageWidth = this.textRenderer.width(message);
        
        final float x = this.widget.getX() + (this.widget.getWidth() / 2) - (messageWidth / 2);
        final float y = this.widget.getY() + ((this.widget.getHeight() - (this.textRenderer.lineHeight - 1)) / 2);
        
        if (!stcwr)
        {
            graphics.drawString(this.textRenderer, message, (int)x, (int)y, textColor);
        }
    }
    
    private void fillMyRainbow(GuiGraphics graphics, float delta, boolean adv)
    {
        RenderUtils.rainbowAllTheWay.stripeScale.set(50.0f / 2.0f * this.stripeScale);
        
        RenderUtils.fillRainbow
        (
                graphics, 
            
            this.widget.getX(), this.widget.getY(), 
            
            this.widget.getX() + this.widget.getWidth(), 
            this.widget.getY() + this.widget.getHeight(), 
            
            0,
            
            0.0f, 0.0f, 0.0f, 
            
            (this.previousRainbowOffset + (this.rainbowOffset - 
                this.previousRainbowOffset) * delta) / 20.0f,
            
            adv
        );
    }
}