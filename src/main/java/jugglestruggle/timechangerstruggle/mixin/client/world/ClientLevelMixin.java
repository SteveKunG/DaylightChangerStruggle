package jugglestruggle.timechangerstruggle.mixin.client.world;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import jugglestruggle.timechangerstruggle.client.TimeChangerStruggleClient;
import jugglestruggle.timechangerstruggle.daynight.DayNightGetterType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

/**
 * Client world mixin; the main class that handles our glorious time which cannot
 * be ignored upon our hands
 *
 * @author JuggleStruggle
 * @implNote Created on 26-Jan-2022, Wednesday
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin extends Level
{
    ClientLevelMixin()
    {
        super(null, null, null, null, false, false, 0, 0);
    }

    @Override
    public long getDayTime()
    {
        return TimeChangerStruggleClient.useWorldTime() ? super.getDayTime() : this.tcs_getModifiedTime(DayNightGetterType.DEFAULT, false);
    }

    @Override
    public long dayTime()
    {
        return TimeChangerStruggleClient.useWorldTime() ? super.dayTime() : this.tcs_getModifiedTime(DayNightGetterType.LUNAR, false);
    }

    @Unique
    public long getPreviousTimeOfDay()
    {
        return TimeChangerStruggleClient.useWorldTime() ? super.getDayTime() : this.tcs_getModifiedTime(DayNightGetterType.DEFAULT, true);
    }

    @Unique
    public long getPreviousLunarTime()
    {
        return TimeChangerStruggleClient.useWorldTime() ? super.dayTime() : this.tcs_getModifiedTime(DayNightGetterType.LUNAR, true);
    }

    @Unique
    public long tcs_getModifiedTime(DayNightGetterType executor, boolean previous)
    {
        return TimeChangerStruggleClient.getTimeChanger().getModifiedTime((ClientLevel)(Object)this, executor, previous);
    }

    @Override
    public float getTimeOfDay(float partialTick)
    {
        final var dt = this.dimensionType();

        final var lunarTime = this.dayTime();
        var lunarAngle = dt.timeOfDay(lunarTime);

        if (TimeChangerStruggleClient.smoothButterCycle)
        {
            final var lunarTimePrev = this.getPreviousLunarTime();
            var lunarAnglePrev = dt.timeOfDay(lunarTimePrev);

            // This is important; not having it means that once either previous or current reaches higher
            // than 1.0 in its sky angle it will be reset immediately back to 0 point whatever as a result
            // of MathHelper.fractionalPart in DimensionType.timeOfDay removing whole numbers and causing
            // it to transition backwards; so in the meantime this is the best that I could come up.
            if (lunarAnglePrev > lunarAngle && lunarTime > lunarTimePrev)
            {
                lunarAngle += 1f;
            }
            else if (lunarAngle > lunarAnglePrev && lunarTimePrev > lunarTime)
            {
                lunarAnglePrev += 1f;
            }

            return lunarAnglePrev + (lunarAngle - lunarAnglePrev) * partialTick;
        }
        else
        {
            return lunarAngle;
        }
    }

    /*
    // Debug version; this is the main method that helped with debugging how the celestal / sky angles
    // worked to come up with a non-backwards solution
    @Override
    public float getTimeOfDay(float partialTick)
    {
        final DimensionType dt = this.dimensionType();
    
        final long lunarTime = this.getDayTime();
        final long lunarTimePrev = this.getPreviousLunarTime();
    
        float skyAngle = dt.timeOfDay(lunarTime);
        float skyAnglePrev = dt.timeOfDay(lunarTimePrev);
    
        float deltas = skyAnglePrev + (skyAngle - skyAnglePrev) * partialTick;
    
        if (Keybindings.toggleWorldTimeKey.consumeClick() && lunarTime != lunarTimePrev)
        {
            double dN = Mth.frac((double)lunarTime / 24000.0 - 0.25);
            double dP = Mth.frac((double)lunarTimePrev / 24000.0 - 0.25);
    
            jugglestruggle.timechangerstruggle.TimeChangerStruggle.LOGGER.info
            (
                "lunar: {}, {} | prev: {}, {} | deltas: {} | tickDelta: {} | dN & P: {}, {}",
                lunarTime, skyAngle, lunarTimePrev, skyAnglePrev,
                deltas, partialTick, dN, dP
            );
        }
    
        if ((skyAnglePrev > skyAngle) && (lunarTime > lunarTimePrev)) {
            skyAngle += 1f;
        } else if ((skyAngle > skyAnglePrev) && (lunarTimePrev > lunarTime)) {
            skyAnglePrev += 1f;
        }
    
        // return deltas;
        return skyAnglePrev + (skyAngle - skyAnglePrev) * partialTick;
    }
     */
}