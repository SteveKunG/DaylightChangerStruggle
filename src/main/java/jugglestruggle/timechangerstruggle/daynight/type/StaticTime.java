package jugglestruggle.timechangerstruggle.daynight.type;

import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import jugglestruggle.timechangerstruggle.client.config.property.FancySectionProperty;
import jugglestruggle.timechangerstruggle.client.config.widget.NumericFieldWidgetConfig;
import jugglestruggle.timechangerstruggle.client.screen.TimeChangerScreen;
import jugglestruggle.timechangerstruggle.client.widget.ButtonWidgetEx;
import jugglestruggle.timechangerstruggle.config.property.BaseProperty;
import jugglestruggle.timechangerstruggle.config.property.LongValue;
import jugglestruggle.timechangerstruggle.daynight.DayNightCycleBasis;
import jugglestruggle.timechangerstruggle.daynight.DayNightCycleBuilder;
import jugglestruggle.timechangerstruggle.daynight.DayNightGetterType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * A time which the user defines and remains as it is.
 *
 * <p> Essentially, this should be assumed as if the daylight cycle
 * is off and the time is set through the time command.
 *
 * @author JuggleStruggle
 * @implNote Created on 26-Jan-2022, Wednesday
 */
@Environment(EnvType.CLIENT)
public class StaticTime implements DayNightCycleBasis
{
    public long timeSet = 0;

    @Override
    public long getModifiedTime(ClientLevel world, DayNightGetterType executor, boolean previous)
    {
        return this.timeSet;
    }

    @Override
    public long getCachedTime()
    {
        return this.timeSet;
    }

    @Override
    public Class<?> getBuilderClass()
    {
        return Builder.class;
    }

    @Override
    public GuiEventListener[] createQuickOptionElements(TimeChangerScreen screen)
    {
        final var propsCreated = this.createProperties().iterator();

        final var sectionProp = (FancySectionProperty)propsCreated.next();
        final var sectionRoughLang = ((TranslatableContents)sectionProp.get().getContents()).getKey();

        final var worldTimeProp = (NumericFieldWidgetConfig<Long>)((LongValue)propsCreated.next()).createConfigElement(screen, sectionProp);

        var setTimes = Lists.newArrayList(PresetSetTimes.values()).stream()
                .filter(PresetSetTimes::shouldShowInQuickOptions).toList();

        final var setTimesSize = setTimes.size();
        final var dayCycles = new ButtonWidgetEx[setTimesSize];

        var i = 0;
        for (PresetSetTimes entry : setTimes)
        {
            final var displayText = entry.getQuickOptionsText();
            final var cycleName = entry.name().toLowerCase(Locale.ROOT);

            dayCycles[i] = new ButtonWidgetEx(20, 20, displayText, Component.translatable(sectionRoughLang + ".worldtime." + cycleName), null, Minecraft.getInstance().font, b ->
            {
                Long value = entry.getTime();
                Long baseTime = 0L;

                final var shiftHeld = Screen.hasShiftDown();
                final var controlHeld = Screen.hasControlDown();
                final var altHeld = Screen.hasAltDown();

                if (shiftHeld || controlHeld || altHeld)
                {
                    baseTime = worldTimeProp.getProperty().get();

                    // Shift just adds so no reason to do anything
                    // Control sets the value negative
                    if (controlHeld)
                    {
                        value = -value;
                    }
                    // Alt divides the value to half
                    if (altHeld)
                    {
                        value /= 2L;
                    }
                }

                final var finalValue = baseTime + value;
                worldTimeProp.setValue(Long.toString(finalValue));
            });

            ++i;
        }
        worldTimeProp.setWidth(148 - 20 * setTimesSize);

        var itemsToAdd = new GuiEventListener[1 + setTimesSize];
        itemsToAdd[0] = worldTimeProp;

        return ArrayUtils.insert(1, itemsToAdd, dayCycles);
    }

    @Override
    public Set<BaseProperty<?, ?>> createProperties()
    {
        ImmutableSet.Builder<BaseProperty<?, ?>> prop = ImmutableSet.builderWithExpectedSize(2);

        final var sectLang = "jugglestruggle.tcs.dnt.statictime.properties.";

        prop.add(new FancySectionProperty("time", Component.translatable(sectLang + "time")));
        prop.add(new LongValue("worldtime", this.timeSet, null, null));

        return prop.build();
    }

    @Override
    public void writePropertyValueToCycle(BaseProperty<?, ?> property)
    {
        final var belongingKey = property.property();

        if (property instanceof LongValue && belongingKey.equals("worldtime"))
        {
            this.timeSet = ((LongValue)property).get();
        }
    }

    public enum PresetSetTimes
    {
        NOON(6000L), MIDNIGHT(18000L), SUNRISE(0L), SUNSET(12000L), DAY(1000L, false, true), NIGHT(13000L, false, true);

        private final long time;
        private final boolean showInQuickOptions;
        private final boolean showInCommand;

        PresetSetTimes(long time)
        {
            this(time, true, true);
        }

        PresetSetTimes(long time, boolean showInQuickOptions, boolean showInCommand)
        {
            this.time = time;
            this.showInQuickOptions = showInQuickOptions;
            this.showInCommand = showInCommand;
        }

        public long getTime()
        {
            return this.time;
        }

        public boolean shouldShowInCommand()
        {
            return this.showInCommand;
        }

        public boolean shouldShowInQuickOptions()
        {
            return this.showInQuickOptions;
        }

        public Component getQuickOptionsText()
        {
            return switch (this)
            {
                case NOON -> Component.literal("\u2600");
                case MIDNIGHT -> Component.literal("\u263D");
                case SUNRISE -> Component.literal("\u25D3");
                case SUNSET -> Component.literal("\u25D2");

                default -> Component.empty();
            };
        }
    }

    public static class Builder implements DayNightCycleBuilder
    {
        @Override
        public DayNightCycleBasis create()
        {
            return new StaticTime();
        }

        @Override
        public String getKeyName()
        {
            return "statictime";
        }

        @Override
        public Component getTranslatableName()
        {
            return Component.translatable("jugglestruggle.tcs.dnt.statictime");
        }

        @Override
        public Component getTranslatableDescription()
        {
            return Component.translatable("jugglestruggle.tcs.dnt.statictime.description");
        }

        @Override
        public boolean hasOptionsToEdit()
        {
            return true;
        }
    }
}
