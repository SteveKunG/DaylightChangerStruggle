package jugglestruggle.timechangerstruggle.client.util.color;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * A base class used exactly to help with RGB colorization rather
 * than create hacky elements when it can be combined all at once!
 *
 * @author JuggleStruggle
 * @implNote Created on 11-Feb-2022, Friday
 */
@Environment(EnvType.CLIENT)
public abstract class AbstractRGB
{
    protected int color;
    protected int previousColor;

    public void tick()
    {}

    public int getInterpolatedColor(float delta)
    {
        return AbstractRGB.getInterpolatedColor(this.previousColor, this.color, delta);
    }

    public int getColor()
    {
        return this.color;
    }

    public int getPrevColor()
    {
        return this.previousColor;
    }

    public void setColor(int color)
    {
        this.color = color;
    }

    public void setPrevColor(int color)
    {
        this.previousColor = color;
    }

    public static int getInterpolatedColor(int prevColor, int color, float delta)
    {
        var currA = color >> 24 & 0xFF;
        var currR = color >> 16 & 0xFF;
        var currG = color >> 8 & 0xFF;
        var currB = color & 0xFF;

        var prevA = prevColor >> 24 & 0xFF;
        var prevR = prevColor >> 16 & 0xFF;
        var prevG = prevColor >> 8 & 0xFF;
        var prevB = prevColor & 0xFF;

        var a = (int)(prevA + ((float)currA - (float)prevA) * delta);
        var r = (int)(prevR + ((float)currR - (float)prevR) * delta);
        var g = (int)(prevG + ((float)currG - (float)prevG) * delta);
        var b = (int)(prevB + ((float)currB - (float)prevB) * delta);

        if (a < 0x00)
        {
            a = 0x00;
        }
        else if (a > 0xFF)
        {
            a = 0xFF;
        }

        if (r < 0x00)
        {
            r = 0x00;
        }
        else if (r > 0xFF)
        {
            r = 0xFF;
        }

        if (g < 0x00)
        {
            g = 0x00;
        }
        else if (g > 0xFF)
        {
            g = 0xFF;
        }

        if (b < 0x00)
        {
            b = 0x00;
        }
        else if (b > 0xFF)
        {
            b = 0xFF;
        }

        return a << 24 | r << 16 | g << 8 | b;

    }
}
