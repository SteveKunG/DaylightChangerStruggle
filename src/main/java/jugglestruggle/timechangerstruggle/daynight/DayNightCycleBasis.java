package jugglestruggle.timechangerstruggle.daynight;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import jugglestruggle.timechangerstruggle.client.config.property.FancySectionProperty;
import jugglestruggle.timechangerstruggle.client.config.widget.WidgetConfigInterface;
import jugglestruggle.timechangerstruggle.client.screen.TimeChangerScreen;
import jugglestruggle.timechangerstruggle.config.property.BaseProperty;
import jugglestruggle.timechangerstruggle.config.property.IntValue;
import jugglestruggle.timechangerstruggle.config.property.StringValue;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/**
 *
 * @author JuggleStruggle
 * @implNote
 * Created on 26-Jan-2022, Wednesday
 */
public interface DayNightCycleBasis
{
    /**
     * Gets the time that the client world should be using.
     *
     * <p> Note: This does not affect the server or the world
     * property in any capacity as it just overrides a getter
     * to apply its own.
     *
     * @param world the client world getting the modified time
     * @param executor the part of the code that called the time
     * @param previous the last modified time, primarily used
     *                  for ticking
     *
     * @return a {@code long} value representing the game time
     */
    long getModifiedTime(ClientLevel world, DayNightGetterType executor, boolean previous);

    /**
     * Returns the cached time provided by the cycle itself.
     *
     * <p> It is used on {@link TimeChangerScreen} to get the time
     * without having to constantly call {@link #getModifiedTime}.
     *
     * @return a {@code long} value representing the game time
     */
    long getCachedTime();

    /**
     * Returns the class which builds this day night cycle.
     *
     * <p> Required for checking if the builder in question is
     * the same as what is going to be checked.
     *
     * @return a builder class
     */
    Class<?> getBuilderClass();

    /**
     * Called whenever a tick occurs.
     */
    default void tick()
    {}

    /**
     * By default when rendering the "date over ticks" option,
     * add a day to represent the next day. This is mostly buggy
     * if used on day 0 without adding a day. It's just Java
     * things™...
     *
     * @return a boolean value
     */
    default boolean shouldAddDayForDateDisplay()
    {
        return true;
    }

    /**
     * Shows an element if this cycle has an option that the user
     * should have quick access to.
     *
     * <p> You should at least be sure that all of the elements
     * combined do fit on width of 150 and the height of 20.
     *
     * @return a list of elements to create.
     */
    default GuiEventListener[] createQuickOptionElements(TimeChangerScreen screen)
    {
        return null;
    }

    /**
     * Creates properties for use in reads/writes, saves, commands
     * and in graphical interfaces such as the Property List.
     *
     * @return a set; should use {@link ImmutableSet} to avoid
     * writes although none of it really matters as it is not
     * written to, but read from
     */
    default Set<BaseProperty<?, ?>> createProperties()
    {
        return null;
    }

    /**
     * Writes the current property back to the current cycle that
     * handles the current time.
     *
     * @param property the property which was overriden to provide
     * the new value.
     *
     * <p>The property string is obtained by using
     * {@link BaseProperty#property()} and is instead used to allow
     * switch statements to be used for quicker value setting. To
     * verify that the type (like the property type obtained) is
     * correct, perform an {@code instanceof} check on the property
     * and then get its property name and apply the value to avoid
     * crash problems and it is also quicker that way.
     *
     * @param <B> the base property, usually represents a type,
     *             like {@link StringValue} or {@link IntValue}
     * @param <V> the type used from the property itself, like
     *             {@link String} or {@link Integer}
     */
    default void writePropertyValueToCycle(BaseProperty<?, ?> property)
    {}

    /**
     * Rearranges {@link #createProperties()}'s given elements to
     * a daylight cycle's own version of the rearrangement. This
     * helps in not having to do all of the elements on itself.
     *
     * @param entry the entry to which is used to help make the
     *         rows of the list
     * @param elementsPerRow how many elements will there be for
     *         each row
     *
     * @return a doubled array of {@link GuiEventListener} which represents
     * the following:
     * <ul>
     * <li> First array:  the row for the list when creating the row
     * <li> Second array: the elements to which the row will utilize
     * </ul>
     */
    default WidgetConfigInterface<?, ?>[][] rearrangeSectionElements(Map.Entry<FancySectionProperty, List<WidgetConfigInterface<?, ?>>> entry, int elementsPerRow)
    {
        final var elements = entry.getValue();

        final var sectionElements = elements.size();
        final var sectionElementsHalf = Mth.ceil((float)sectionElements / (float)elementsPerRow);

        var sectionPartsToCreate = new WidgetConfigInterface<?, ?>[sectionElementsHalf][elementsPerRow];

        for (var i = 0; i < sectionElements; ++i)
        {
            WidgetConfigInterface<?, ?> entryElement = elements.get(i);

            final var rowToPut = i / elementsPerRow;
            final var elementOrder = i % elementsPerRow;

            sectionPartsToCreate[rowToPut][elementOrder] = entryElement;
        }

        return sectionPartsToCreate;
    }

    default void rearrangeCreatedOptionElements(int x, int y, int entryWidth, int entryHeight, List<WidgetConfigInterface<?, ?>> myConfigElements)
    {
        final var halfWidth = entryWidth / 2;
        final var xCentered = x + halfWidth;
        final var myElemsSize = myConfigElements.size();

        switch (myElemsSize)
        {
            case 1:
            {
                WidgetConfigInterface<?, ?> elem = myConfigElements.get(0);

                if (elem instanceof AbstractWidget elemClickable)
                {
                    elemClickable.setX(xCentered - (halfWidth - 4));
                    elemClickable.setY(y + 2);

                    if (elemClickable instanceof EditBox)
                    {
                        elemClickable.setX(elemClickable.getX() + 1);
                        elemClickable.setY(elemClickable.getY() + 1);
                        elemClickable.setWidth(entryWidth - 12);
                    }
                    else
                    {
                        elemClickable.setWidth(entryWidth - 10);
                    }
                }

                break;
            }
            case 2:
            {
                final var entryWidthDiv = halfWidth - 8;

                for (var i = 1; i >= 0; --i)
                {
                    WidgetConfigInterface<?, ?> elem = myConfigElements.get(i);

                    if (elem instanceof AbstractWidget elemClickable)
                    {
                        elemClickable.setX(xCentered + 1);
                        elemClickable.setY(y + 2);

                        if (i % 2 == 0)
                        {
                            elemClickable.setX(elemClickable.getX() - (halfWidth - 4));
                        }
                        else
                        { // 1
                            elemClickable.setX(elemClickable.getX() + 2);
                        }

                        if (elemClickable instanceof EditBox)
                        {
                            elemClickable.setX(elemClickable.getX() + 1);
                            elemClickable.setY(elemClickable.getY() + 1);
                            elemClickable.setWidth(entryWidthDiv - 2);
                        }
                        else
                        {
                            elemClickable.setWidth(entryWidthDiv);
                        }
                    }

                }

                break;
            }
            case 3:
            {
                final var entryWidthDiv = entryWidth / myElemsSize - 8;
                final var entryWidthDivSeparator = entryWidthDiv + 4;

                for (var i = myElemsSize - 1; i >= 0; --i)
                {
                    WidgetConfigInterface<?, ?> elem = myConfigElements.get(i);

                    if (elem instanceof AbstractWidget elemClickable)
                    {
                        var xOffset = entryWidthDivSeparator * i;

                        elemClickable.setX(xCentered + xOffset - (int)(entryWidthDivSeparator * 1.5f));
                        elemClickable.setY(y + 2);

                        if (elemClickable instanceof EditBox)
                        {
                            elemClickable.setX(elemClickable.getX() + 1);
                            elemClickable.setY(elemClickable.getY() + 1);
                            elemClickable.setWidth(entryWidthDiv - 2);
                        }
                        else
                        {
                            elemClickable.setWidth(entryWidthDiv);
                        }
                    }

                }

                break;
            }
        }
    }
}
