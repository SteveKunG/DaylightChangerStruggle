package jugglestruggle.timechangerstruggle.daynight.type;

import java.util.Calendar;

import jugglestruggle.timechangerstruggle.daynight.DayNightCycleBasis;
import jugglestruggle.timechangerstruggle.daynight.DayNightCycleBuilder;
import jugglestruggle.timechangerstruggle.daynight.DayNightGetterType;
import jugglestruggle.timechangerstruggle.util.DaylightUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

/**
 *
 * @author JuggleStruggle
 * @implNote Created on 26-Jan-2022, Wednesday
 */
@Environment(EnvType.CLIENT)
public class SystemTime implements DayNightCycleBasis
{
    private long cachedTime;
    private long previousCachedTime;

    public SystemTime()
    {
        this.tick();
    }

    @Override
    public void tick()
    {
        var now = Calendar.getInstance();

        // int year = now.get(Calendar.YEAR);
        var doy = now.get(Calendar.DAY_OF_YEAR);

        // TODO: Account for the current time instead of relying time in a day
        // int year = now.get(Calendar.YEAR);
        long hour = now.get(Calendar.HOUR_OF_DAY);
        double mins = now.get(Calendar.MINUTE);
        double secs = now.get(Calendar.SECOND);

        hour -= 6;

        // Since hour is less than 0 (subtracted from 6) then add 24 (which
        // in the end subtracts 24 from hour as hour is negative) to get a
        // previous' day of time
        if (hour < 0)
        {
            hour += 24;
        }

        mins = mins * DaylightUtils.ONE_MINUTE;
        secs = secs * DaylightUtils.ONE_SECOND;

        this.previousCachedTime = this.cachedTime;
        // TODO: Do something better here in minutes and seconds
        // this.cachedTime = (hour * (long)DaylightUtils.ONE_HOUR) + (long)(mins + secs);
        this.cachedTime = /* ((long)year * (long)DaylightUtils.ONE_YEAR) + */doy * (long)DaylightUtils.ONE_DAY + hour * (long)DaylightUtils.ONE_HOUR + (long)(mins + secs);
    }

    @Override
    public long getModifiedTime(ClientLevel world, DayNightGetterType executor, boolean previous)
    {
        return previous ? this.previousCachedTime : this.cachedTime;
    }

    @Override
    public long getCachedTime()
    {
        return this.cachedTime;
    }

    @Override
    public boolean shouldAddDayForDateDisplay()
    {
        return false;
    }

    @Override
    public Class<?> getBuilderClass()
    {
        return Builder.class;
    }

    public static class Builder implements DayNightCycleBuilder
    {
        @Override
        public DayNightCycleBasis create()
        {
            return new SystemTime();
        }

        @Override
        public String getKeyName()
        {
            return "systemtime";
        }

        @Override
        public Component getTranslatableName()
        {
            return Component.translatable("jugglestruggle.tcs.dnt.systemtime");
        }

        @Override
        public Component getTranslatableDescription()
        {
            return Component.translatable("jugglestruggle.tcs.dnt.systemtime.description");
        }
    }
}
