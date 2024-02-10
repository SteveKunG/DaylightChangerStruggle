package jugglestruggle.timechangerstruggle.client.screen;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.systems.RenderSystem;

import jugglestruggle.timechangerstruggle.client.TimeChangerStruggleClient;
import jugglestruggle.timechangerstruggle.client.config.property.FancySectionProperty;
import jugglestruggle.timechangerstruggle.client.config.widget.CyclingWidgetConfig;
import jugglestruggle.timechangerstruggle.client.config.widget.WidgetConfigInterface;
import jugglestruggle.timechangerstruggle.client.widget.ButtonWidgetEx;
import jugglestruggle.timechangerstruggle.client.widget.CyclingButtonWidgetEx;
import jugglestruggle.timechangerstruggle.client.widget.PositionedTooltip;
import jugglestruggle.timechangerstruggle.config.property.BaseProperty;
import jugglestruggle.timechangerstruggle.daynight.DayNightCycleBasis;
import jugglestruggle.timechangerstruggle.daynight.DayNightCycleBuilder;
import jugglestruggle.timechangerstruggle.util.DaylightUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

/**
 *
 * @author JuggleStruggle
 * @implNote Created on 26-Jan-2022, Wednesday
 */
@Environment(EnvType.CLIENT)
public class TimeChangerScreen extends Screen
{
    private static final Predicate<GuiEventListener> ORDERABLE_TOOLTIP_PREDICATE = ClientTooltipComponent.class::isInstance;
    private Menu currentMenu = Menu.MAIN_MENU;

    /**
     * To avoid losing our elements based on the menu, we cache the elements
     * here instead of relying on {@link Screen}'s as it will clear every single
     * time we resize the screen or whatever incentivizes it to get the elements
     * cleared.
     *
     * <p> Plus it has the advantage of keeping the elements whenever we switch
     * the menus, as that is used throughout this screen.
     */
    private final Map<Menu, List<GuiEventListener>> menuElements = new EnumMap<>(Menu.class);

    /**
     * To avoid the need of needless saving, this option is used whenever the
     * user modifies anything relating to a setting.
     */
    private boolean menuDirty = false;

    /**
     * Whenever a property that calls {@link #consumeChangedProperty} wants to
     * tell this method that it has changed, there is a chance that it causes a
     * hard-loop crash due to the property being in quick-options from the
     * properties menu.
     */
    private boolean propertyBeingChanged = false;

    /**
     * When the user exits the screen (through forced or voluntary means), save
     * the current cycle to disk. This is only used if the daylight cycle has
     * quick options, like Static Time, and makes a change to the current cycle.
     *
     * <p> This is cleared if the menu is {@link Menu#SWITCH_DAYLIGHT_CYCLE_MENU}
     * and either switches the active daylight cycle to something else or performs
     * property updates using the active daylight cycle.
     */
    private Boolean mainMenu_saveCurrentOption = null;

    /**
     * As the name suggests, whenever the menu is on the daylight cycle, avoid
     * rendering the main daylight cycle list and go forward only rendering the
     * properties list instead.
     *
     * <p> If the screen resolution is more than enough to render the cycle
     * switch list and the properties list then leaving this field to {@code null}
     * renders both. Otherwise having it set to {@code false} while it is more
     * than enough only renders the cycle switch list and {@code true} renders
     * only the property list.
     *
     * <p> In order to only render the properties list, the following must occur:
     * <ul>
     * <li> The property list must exist: this is obvious as without it you can
     * expect a game crash as a result
     * <li> Property list must not be empty: otherwise why are we wasting the
     * user's time by showing them nothing?
     * </ul>
     */
    private Boolean switchDaylightCycleMenu_onlyPropertiesList;

    /**
     * Returns a boolean stating if there can be two lists rendering at once.
     */
    private BooleanSupplier switchDaylightCycleMenu_canRenderTwoLists;
    /**
     * Returns a boolean stating if the property list exists; it is not really
     * used that much as there is a conflict as to which method should be used.
     *
     * <p> Only use this if the actual menu is
     * {@link Menu#SWITCH_DAYLIGHT_CYCLE_MENU}!
     */
    private BooleanSupplier switchDaylightCycleMenu_propertiesListExists;
    /**
     * Gets the property list from a list of elements. This "list of elements"
     * should really be from {@link #menuElements}'s key which is
     * {@link Menu#SWITCH_DAYLIGHT_CYCLE_MENU}.
     */
    private Function<List<GuiEventListener>, SwitchDaylightCyclePropertyList> switchDaylightCycleMenu_getPropertyListFunc;

    /**
     * Used when this screen is opened with the intention that only the properties
     * list will be used. This is restrictive compared to it not being the case as
     * the Back button closes and also removes switching the menu list.
     *
     * <p> ESC exits the screen instead of being sent back into the main menu.
     */
    private DayNightCycleBuilder switchDaylightCycleMenu_propertiesListBuilder;

    public TimeChangerScreen()
    {
        super(Component.empty());
    }

    public TimeChangerScreen(DayNightCycleBuilder builder)
    {
        this();

        this.currentMenu = Menu.SWITCH_DAYLIGHT_CYCLE_MENU;
        this.switchDaylightCycleMenu_propertiesListBuilder = builder;
    }

    @Override
    protected void init()
    {
        this.initSelf();
    }

    @SuppressWarnings("unchecked")
    private <T extends GuiEventListener & Renderable & NarratableEntry> void initSelf()
    {
        // Elements that aren't disposable and persists after a child clear, can also persist
        // after a menu change unless the menu has the option to clear all elements after a
        // change
        List<GuiEventListener> elements = null;
        // Elements that are disposable and have no need to cache after a call to initSelf
        List<T> disposableElements = null;

        var wasMenuScreenCreated = false;

        if (this.menuElements.containsKey(this.currentMenu))
        {
            elements = this.menuElements.get(this.currentMenu);
        }
        else
        {
            switch (this.currentMenu)
            {
                case MAIN_MENU:
                {
                    elements = new ArrayList<>(6);

                    // World Time
                    elements.add(TimeChangerScreen.createCyclingWidget(150, 20, Component.translatable("jugglestruggle.tcs.screen.toggleworldtime"), TimeChangerStruggleClient.worldTime, this::toggleWorldTime, a -> TimeChangerScreen.createTooltips(this.font, (byte)0, Component.translatable("jugglestruggle.tcs.screen.toggleworldtime.desc"), null)));
                    // Date Over Ticks
                    elements.add(TimeChangerScreen.createCyclingWidget(150, 20, Component.translatable("jugglestruggle.tcs.screen.toggledate"), TimeChangerStruggleClient.dateOverTicks, this::toggleDateVsTicks, a -> TimeChangerScreen.createTooltips(this.font, (byte)0, Component.translatable("jugglestruggle.tcs.screen.toggledate.desc"), null)));

                    // Smooth-Butter Daylight Cycle
                    elements.add(TimeChangerScreen.createCyclingWidget(150, 20, Component.translatable("jugglestruggle.tcs.screen.togglesmoothbutterdaylightcycle"), TimeChangerStruggleClient.smoothButterCycle, this::toggleSmoothButterDaylightCycle, a -> TimeChangerScreen.createTooltips(this.font, (byte)0, Component.translatable("jugglestruggle.tcs.screen.togglesmoothbutterdaylightcycle.desc"), null)));
                    // Switch Cycle Menu
                    elements.add(new ButtonWidgetEx(150, 20, this.mainMenu_getButtonWidgetText_switchGetterMenu(), this.mainMenu_getButtonWidgetTooltipFirstLine_switchGetterMenu(), Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.desc"), this.font, b ->
                    {
                        this.updateMenuType(Menu.SWITCH_DAYLIGHT_CYCLE_MENU);
                    }));
                    // Quick-Switch Cycle
                    elements.add(new ButtonWidgetEx(20, 20, Component.nullToEmpty("\u21C6"), this.mainMenu_getButtonWidgetTooltipFirstLine_switchGetterMenu(), Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.quick.desc"), this.font, this::mainMenu_quickSwitchDaylightCycleType));

                    // Configuration (not used yet as of version 0.0.0)
                    elements.add(Button.builder(Component.empty(), btn ->
                    {
                    }).bounds(0, 0, 0, 0).build());

                    this.mainMenu_createQuickOptionElements(elements);

                    break;
                }
                case SWITCH_DAYLIGHT_CYCLE_MENU:
                {
                    final var propListOnly = this.switchDaylightCycleMenu_propertiesListBuilder != null;

                    if (propListOnly)
                    {
                        this.switchDaylightCycleMenu_canRenderTwoLists = () -> false;
                    }
                    else
                    {
                        this.switchDaylightCycleMenu_canRenderTwoLists = () -> this.width >= 460;
                    }

                    this.switchDaylightCycleMenu_getPropertyListFunc = elementsInMenu ->
                    {
                        final var size = propListOnly ? 2 : 5;

                        if (elementsInMenu.size() >= size)
                        {
                            var assumedPropertyList = elementsInMenu.get(size - 1);
                            return assumedPropertyList instanceof SwitchDaylightCyclePropertyList ? (SwitchDaylightCyclePropertyList)assumedPropertyList : null;
                        }

                        return null;
                    };

                    this.switchDaylightCycleMenu_propertiesListExists = () ->
                    {
                        var elementsInMenu = this.menuElements.get(Menu.SWITCH_DAYLIGHT_CYCLE_MENU);
                        return this.switchDaylightCycleMenu_getPropertyListFunc.apply(elementsInMenu) != null;
                    };

                    var cycleBasis = TimeChangerStruggleClient.getTimeChanger();
                    var cycleChangerExists = cycleBasis != null;

                    if (propListOnly && (!cycleChangerExists || !this.switchDaylightCycleMenu_propertiesListBuilder.getClass().equals(cycleBasis.getBuilderClass())))
                    {
                        cycleBasis = this.switchDaylightCycleMenu_propertiesListBuilder.create();
                        TimeChangerStruggleClient.config.createOrModifyDaylightCycleConfig(cycleBasis, false);

                        cycleChangerExists = true;
                    }

                    elements = new ArrayList<>(propListOnly ? 2 : 3 + (cycleChangerExists ? 2 : 1));

                    // Back (Index 0)
                    var button = new ButtonWidgetEx(20, 18, Component.nullToEmpty("\u2190"), Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.back"), null, this.font, b ->
                    {
                        this.onClose();
                    });
                    elements.add(button);

                    if (!propListOnly)
                    {
                        // Cycle List Switch (Index 1) (Not there if prop list only is shown)
                        // Switch between two cycle menu list (Allows one to only see one list instead
                        // of the primary list all the time; only shown if cycle property list exists)
                        elements.add(TimeChangerScreen.createCyclingWidget(20, 18, Component.empty(), cycleChangerExists, true, Component.nullToEmpty("\u21C4"), Component.nullToEmpty("\u21C4"), this::toggleCycleMenuListVisibility, a -> TimeChangerScreen.createTooltips(this.font, (byte)2, this.switchDaylightCycleMenu_switchSoloCycleList_getTooltipFirstLine(), Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.switchsololist.desc"))));

                        // Dual List Switch (Index 2) (Not there if prop list only is shown)
                        // Switch between two lists to one which nulls and un-nulls the only boolean value >:)
                        elements.add(TimeChangerScreen.createCyclingWidget(20, 18, Component.empty(), this.switchDaylightCycleMenu_canRenderTwoLists.getAsBoolean(), true, Component.nullToEmpty("\u275A\u275A"), Component.nullToEmpty("\u275A"), this::toggleCycleMenuDualList, a -> TimeChangerScreen.createTooltips(this.font, (byte)2, this.switchDaylightCycleMenu_switchDualListVisibility_getTooltipFirstLine(), Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.switchduallist.desc"))));

                        // Daylight Cycle List (Index 3) (Not there if prop list only is shown)
                        elements.add(new SwitchGetterBuilderList(this));
                    }

                    // Property List (Index 4) (Index 1 if prop list only is shown)
                    if (cycleChangerExists)
                    {
                        this.switchDaylightCycleMenu_buildConfigFromType(cycleBasis, elements, true);
                    }

                    break;
                }
                // (not used yet as of version 0.0.0)
                case CONFIGURATION_MENU:
                {

                    break;
                }
            }

            if (elements != null)
            {
                this.menuElements.put(this.currentMenu, elements);
                wasMenuScreenCreated = true;
            }
        }

        if (elements == null || elements.size() <= 0)
        {
            return;
        }

        final var w = this.width / 2;
        final var xL = w - 152;
        final var xR = w + 2;

        var y = this.height - 70;
        var y2 = y + 22;

        // Reposition the elements as it is not done whenever it is created
        switch (this.currentMenu)
        {
            case MAIN_MENU:
            {
                var pw = (AbstractWidget)elements.get(0); // Toggle World Time
                pw.setX(xL);
                pw.setY(y);
                pw = (AbstractWidget)elements.get(1); // Toggle Date
                pw.setX(xL);
                pw.setY(y2);

                pw = (AbstractWidget)elements.get(3); // Switch Getter Menu
                pw.setX(xR);
                pw.setY(y2);
                pw = (AbstractWidget)elements.get(4); // Quick-Switch Getter Type
                pw.setX(xR + 152);
                pw.setY(y2);

                pw = (AbstractWidget)elements.get(5); // Settings
                pw.visible = false;
                pw.active = false;

                final var quickOptionElementsSize = elements.size() - 6;
                final var anyQuickOptionElements = quickOptionElementsSize > 0;

                pw = (AbstractWidget)elements.get(2); // Smooth-Butter Daylight Cycle
                pw.setX(xR);
                pw.setY(y);
                pw.visible = pw.active = !anyQuickOptionElements;

                if (anyQuickOptionElements)
                {
                    // For now just add the elements in its X position and width
                    var xAddition = xR;
                    var wasPreviousTextFieldWidget = false;

                    for (var i = 0; i < quickOptionElementsSize; ++i)
                    {
                        var quickElement = elements.get(i + 6);

                        // Sorry if we don't support different types of widgets :(
                        if (quickElement instanceof AbstractWidget)
                        {
                            pw = (AbstractWidget)quickElement;

                            if (pw instanceof EditBox)
                            {
                                xAddition += 1;
                                pw.setX(xAddition);
                                pw.setY(y + 1);
                                // pw.setWidth(pw.getWidth() - 2);
                                // pw.setHeight(pw.getHeight() - 2);

                                wasPreviousTextFieldWidget = true;
                            }
                            else
                            {
                                if (wasPreviousTextFieldWidget)
                                {
                                    xAddition += 1;
                                }

                                pw.setX(xAddition);
                                pw.setY(y);

                                wasPreviousTextFieldWidget = false;
                            }

                            xAddition += pw.getWidth();
                        }
                    }
                }

                break;
            }

            case SWITCH_DAYLIGHT_CYCLE_MENU:
            {
                final var propListOnly = this.switchDaylightCycleMenu_propertiesListBuilder != null;

                //                final boolean hasTwoLists = elements.size() == 2;
                final var canRenderTwoLists = this.switchDaylightCycleMenu_canRenderTwoLists.getAsBoolean();

                final var availableCycleTypesList = propListOnly ? null : (SwitchGetterBuilderList)elements.get(3);
                final var cyclePropertyList = this.switchDaylightCycleMenu_getPropertyListFunc.apply(elements);
                final var hasTwoLists = availableCycleTypesList != null && cyclePropertyList != null;

                SwitchGetterBasisBuilderList<?>[] elemList = null;

                if (hasTwoLists)
                {
                    if (this.switchDaylightCycleMenu_onlyPropertiesList == null)
                    {
                        if (canRenderTwoLists)
                        {
                            elemList = new SwitchGetterBasisBuilderList[] { availableCycleTypesList, cyclePropertyList
                            };
                        }
                    }
                    else if (this.switchDaylightCycleMenu_onlyPropertiesList && cyclePropertyList.hasAnyElements())
                    {
                        elemList = new SwitchGetterBasisBuilderList[] { cyclePropertyList };
                        availableCycleTypesList.setVisible(false);
                    }

                    if (elemList == null)
                    {
                        cyclePropertyList.setVisible(false);
                    }
                }

                if (elemList == null)
                {
                    if (propListOnly)
                    {
                        elemList = new SwitchGetterBasisBuilderList[] { cyclePropertyList };
                    }
                    else
                    {
                        elemList = new SwitchGetterBasisBuilderList[] { availableCycleTypesList };
                    }
                }

                var listTop = 40;
                var listHeight = this.height - (64 + (hasTwoLists || canRenderTwoLists ? 6 : 0));

                switch (elemList.length)
                {
                    case 1:
                    {
                        SwitchGetterBasisBuilderList<?> lobster = elemList[0];

                        lobster.setX(10);
                        lobster.setTopPos(listTop);
                        lobster.setWidth(this.width - 20);
                        lobster.setHeight(listHeight);

                        break;
                    }
                    case 2:
                    {
                        SwitchGetterBasisBuilderList<?> leftLobster = elemList[0];

                        leftLobster.setX(9);
                        leftLobster.setTopPos(listTop);
                        leftLobster.setWidth(w - 11);
                        leftLobster.setHeight(listHeight);

                        SwitchGetterBasisBuilderList<?> rightLobster = elemList[1];

                        rightLobster.setX(w + 1);
                        rightLobster.setTopPos(listTop);
                        rightLobster.setWidth(w - 11);
                        rightLobster.setHeight(listHeight);

                        break;
                    }
                }

                for (final SwitchGetterBasisBuilderList<?> lobster : elemList)
                {
                    lobster.setVisible(true);
                    lobster.onLocSizeUpdate();

                    if (cyclePropertyList != null && lobster instanceof SwitchDaylightCyclePropertyList)
                    {
                        disposableElements = new ArrayList<>();

                        lobster.setHeight(lobster.getHeight() - 22);

                        // Auto-Save Properties
                        var widgetRenderer = TimeChangerScreen.createCyclingWidget(20, 20, Component.empty(), TimeChangerStruggleClient.applyOnPropertyListValueUpdate, true, null, null, this::toggleCyclePropertyListAutoApply, state ->
                        {
                            final var stateText = state ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;

                            return Tooltip.create(Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.propertylist.autosave.desc.firstline", stateText).append(Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.propertylist.autosave.desc")));

                            //                                return TimeChangerScreen.createOrderedTooltips(this.font, (byte)2,
                            //                                    Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.propertylist.autosave.desc.firstline", stateText),
                            //                                    Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.propertylist.autosave.desc"));

                        });

                        var bwe = (AbstractWidget)widgetRenderer;

                        bwe.setX(lobster.getLeft());
                        bwe.setY(lobster.getBottom() + 2);

                        bwe.setMessage(Component.nullToEmpty(TimeChangerStruggleClient.applyOnPropertyListValueUpdate ? "\u25C9" : "\u25EF"));

                        disposableElements.add((T)bwe);

                        // Cycle Type in Use
                        // Would be funny if the entire thing just crashed just because it failed to find a
                        // cycle builder matching the editing cycle type

                        var cachedBuilderName = TimeChangerStruggleClient.getCachedCycleBuilderByClass(cyclePropertyList.modifyingCycleType.getBuilderClass()).get().getTranslatableName();

                        // Save Properties
                        bwe = new ButtonWidgetEx(74, 20, Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.propertylist.save"), Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.propertylist.save.desc", cachedBuilderName), null, this.font, this::switchDaylightCycleMenu_propertyList_saveProperties);

                        bwe.active = !TimeChangerStruggleClient.applyOnPropertyListValueUpdate;
                        bwe.setX(lobster.getLeft() + 22);
                        bwe.setY(lobster.getBottom() + 2);

                        disposableElements.add((T)bwe);

                        // Reset Properties to Previous State
                        bwe = new ButtonWidgetEx(20, 20, Component.nullToEmpty("\u21BA"), Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.propertylist.reset.desc", cachedBuilderName), null, this.font, this::switchDaylightCycleMenu_propertyList_resetProperties);

                        bwe.active = !TimeChangerStruggleClient.applyOnPropertyListValueUpdate;
                        bwe.setX(lobster.getRight() - 20);
                        bwe.setY(lobster.getBottom() + 2);

                        disposableElements.add((T)bwe);
                    }
                }

                // Back
                var b = (AbstractWidget)elements.get(0);
                b.setX(4);
                b.setY(4);

                if (!propListOnly)
                {
                    // Cycle List Switch
                    b = (AbstractWidget)elements.get(1);
                    b.active = b.visible = hasTwoLists;

                    if (hasTwoLists)
                    {
                        b.setX(w + (canRenderTwoLists ? 2 : -10));
                        b.setY(this.height - 24);

                        if (canRenderTwoLists)
                        {
                            b.active = this.switchDaylightCycleMenu_onlyPropertiesList != null;
                        }
                    }

                    // Dual List Switch
                    b = (AbstractWidget)elements.get(2);
                    b.active = b.visible = canRenderTwoLists;

                    if (canRenderTwoLists)
                    {
                        b.setX(w + (hasTwoLists ? -22 : -10));
                        b.setY(this.height - 24);

                        b.active = hasTwoLists;
                    }
                }

                break;
            }

            case CONFIGURATION_MENU:
            {
                break;
            }
        }

        elements.stream().forEach(elem ->
        {
            this.addRenderableWidget((T)elem);
        });

        if (disposableElements != null && !disposableElements.isEmpty())
        {
            disposableElements.stream().forEach(dispElem ->
            {
                this.addRenderableWidget(dispElem);
            });
        }

        if (wasMenuScreenCreated && this.currentMenu == Menu.SWITCH_DAYLIGHT_CYCLE_MENU)
        {
            final var propListOnly = this.switchDaylightCycleMenu_propertiesListBuilder != null;

            if (!propListOnly)
            {
                ((SwitchGetterBuilderList)elements.get(3)).addSectionEntries();
            }
        }
    }

    @Override
    public void onClose()
    {
        switch (this.currentMenu)
        {
            case CONFIGURATION_MENU:
            case SWITCH_DAYLIGHT_CYCLE_MENU:
            {
                if (this.currentMenu == Menu.SWITCH_DAYLIGHT_CYCLE_MENU)
                {
                    this.switchDaylightCycleMenu_savePropertiesToConfig();
                }

                if (this.menuElements.containsKey(Menu.MAIN_MENU))
                {
                    this.updateMenuType(Menu.MAIN_MENU);
                    return;
                }
                else
                {
                    break;
                }
            }

            default:
                break;
        }

        super.onClose();
    }

    @Override
    public void removed()
    {
        this.menuElements.clear();

        this.mainMenu_saveQuickOptionElements();

        if (this.menuDirty)
        {
            TimeChangerStruggleClient.config.writeIfModified();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta)
    {
        switch (this.currentMenu)
        {
            case MAIN_MENU:
            {
                if (this.minecraft.level != null)
                {
                    var parsedTime = DaylightUtils.getParsedTime(this.minecraft.level, TimeChangerStruggleClient.dateOverTicks);

                    var textWidth = this.font.width(parsedTime);

                    var x = this.width / 2;
                    var y = this.height - 86;

                    graphics.fillGradient(x - 152, y, x + 152, y + this.font.lineHeight + 4, 0xAA000000, 0x77000000);
                    graphics.drawString(this.font, parsedTime, x - textWidth / 2, y + 3, -1);
                }

                break;
            }

            default:
                break;
        }

        // Renders the elements
        super.render(graphics, mouseX, mouseY, delta);

        // Renders anything in front of the elements, this also includes the tooltips
        TimeChangerScreen.renderTooltips(graphics, this, this, mouseX, mouseY, 0, 0);

        switch (this.currentMenu)
        {
            case SWITCH_DAYLIGHT_CYCLE_MENU:
            {
                Optional<? extends GuiEventListener> foundElement = this.children().stream().filter(elemToCheck ->
                {
                    if (elemToCheck instanceof SwitchGetterBasisBuilderList)
                    {
                        SwitchGetterBasisBuilderList<?> elemList = (SwitchGetterBasisBuilderList<?>)elemToCheck;
                        return elemList.isVisible() && elemList.isMouseOver(mouseX, mouseY);
                    }
                    return false;
                }).findFirst();

                if (foundElement.isPresent())
                {
                    SwitchGetterBasisBuilderList<?> listObtained = (SwitchGetterBasisBuilderList<?>)foundElement.get();

                    listObtained.renderTooltips(graphics, mouseX, mouseY);
                }

                break;
            }

            default:
                break;
        }
    }

    @Override
    public void renderTransparentBackground(GuiGraphics guiGraphics)
    {}

    @Override
    public void tick()
    {
        this.children().forEach(elem ->
        {
            if (elem instanceof SwitchGetterBasisBuilderList)
            {
                ((SwitchGetterBasisBuilderList<?>)elem).tick();
            }
        });
    }

    //
    // Global Methods
    // Not exclusive to a single menu, so it can be shared
    //
    private void updateMenuType(Menu menuToChangeTo)
    {
        // Up to this point the "current menu" is the previous menu
        if (this.currentMenu.clearMenuOnSwitch())
        {
            this.menuElements.get(this.currentMenu).clear();
            this.menuElements.remove(this.currentMenu);

            if (this.currentMenu == Menu.SWITCH_DAYLIGHT_CYCLE_MENU)
            {
                this.switchDaylightCycleMenu_canRenderTwoLists = null;
                this.switchDaylightCycleMenu_propertiesListExists = null;
                this.switchDaylightCycleMenu_getPropertyListFunc = null;
            }
        }

        // And now the "current menu" IS the current menu!
        this.currentMenu = menuToChangeTo;

        // Clear children elements to avoid duplicates and clean temporary elements
        this.clearWidgets();
        // Then rinse and repeat :)
        this.init();
    }

    public <B extends BaseProperty<B, V>, V> void consumeChangedProperty(B owningProperty, V newValue)
    {
        // Avoid a crashing loop whenever properties are being changed (only occurs when attempting to
        // synchronize with quick option's widgets)
        if (this.propertyBeingChanged)
        {
            return;
        }

        switch (this.currentMenu)
        {
            case MAIN_MENU:
            {
                if (TimeChangerStruggleClient.getTimeChanger() == null)
                {
                    return;
                }

                owningProperty.set(newValue);
                TimeChangerStruggleClient.getTimeChanger().writePropertyValueToCycle(owningProperty);

                this.mainMenu_saveCurrentOption = true;

                break;
            }
            case SWITCH_DAYLIGHT_CYCLE_MENU:
            {
                var shouldApplyToCycle = TimeChangerStruggleClient.applyOnPropertyListValueUpdate;

                var propList = this.switchDaylightCycleMenu_getPropertyList();

                if (propList != null && shouldApplyToCycle)
                {
                    owningProperty.set(newValue);

                    propList.modifyingCycleType.writePropertyValueToCycle(owningProperty);
                    propList.dirty = true;

                    var timeChangerEquals = TimeChangerStruggleClient.getTimeChanger() != null && TimeChangerStruggleClient.getTimeChanger().getBuilderClass().equals(propList.modifyingCycleType.getBuilderClass());

                    if (this.menuElements.containsKey(Menu.MAIN_MENU) && timeChangerEquals)
                    {
                        // Clears the "save current type" if the type equals
                        this.mainMenu_saveCurrentOption = null;

                        var elems = this.menuElements.get(Menu.MAIN_MENU);

                        if (elems != null && elems.size() > 4)
                        {
                            this.propertyBeingChanged = true;

                            for (var i = elems.size() - 1; i >= 4; --i)
                            {
                                var elem = elems.get(i);

                                if (elem instanceof WidgetConfigInterface)
                                {
                                    WidgetConfigInterface<?, ?> elemConfig = (WidgetConfigInterface<?, ?>)elem;

                                    var propertyNameEquals = elemConfig.getProperty().property().equals(owningProperty.property());

                                    if (propertyNameEquals)
                                    {
                                        // If the property name from each properties equals, then it is assumed
                                        // that they both share their value. If the class type isn't the same, then
                                        // I would be wondering why...
                                        if (elemConfig.getProperty().getClass().equals(owningProperty.getClass()))
                                        {
                                            @SuppressWarnings("unchecked")
                                            var config = (WidgetConfigInterface<B, V>)elemConfig;

                                            config.setInitialValue(newValue);
                                            config.setPropertyValueToDefault(true);

                                            // The main reason is that cycling widget already sets the widget value
                                            // to default whenever changing the property to a default value; the rest
                                            // needs both resets
                                            if (!(config instanceof CyclingWidgetConfig))
                                            {
                                                config.forceSetWidgetValueToDefault(true);
                                            }
                                        }

                                        break;
                                    }
                                }
                            }

                            this.propertyBeingChanged = false;
                        }
                    }
                }

                break;
            }

            default:
                break;
        }

        this.menuDirty = true;
    }

    //
    // Main Menu Methods
    //
    private void toggleWorldTime(CycleButton<Boolean> b, boolean newValue)
    {
        TimeChangerStruggleClient.worldTime = newValue;
        this.menuDirty = true;
    }

    private void toggleDateVsTicks(CycleButton<Boolean> b, boolean newValue)
    {
        TimeChangerStruggleClient.dateOverTicks = newValue;
        this.menuDirty = true;
    }

    private void toggleSmoothButterDaylightCycle(CycleButton<Boolean> b, boolean newValue)
    {
        TimeChangerStruggleClient.smoothButterCycle = newValue;
        this.menuDirty = true;
    }

    private Component mainMenu_getButtonWidgetText_switchGetterMenu()
    {
        return Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu", TimeChangerStruggleClient.getCachedCycleTypeSize());
    }

    private Component mainMenu_getButtonWidgetTooltipFirstLine_switchGetterMenu()
    {
        Component usingText = null;

        if (TimeChangerStruggleClient.getTimeChanger() != null)
        {
            var tcBuilder = TimeChangerStruggleClient
                    .getCachedCycleBuilderByClass(TimeChangerStruggleClient.getTimeChanger().getBuilderClass());

            if (tcBuilder.isPresent())
            {
                usingText = tcBuilder.get().getTranslatableName();
            }
        }

        if (usingText == null)
        {
            usingText = Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.desc.using.none");
        }

        return Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.desc.using", usingText);
    }

    private void mainMenu_quickSwitchDaylightCycleType(Button b)
    {
        this.mainMenu_onSwitchDaylightCycleType(true, () ->
        {
            this.mainMenu_saveQuickOptionElements();
            TimeChangerStruggleClient.quickSwitchCachedCycleType(Screen.hasShiftDown());
        });
    }

    void mainMenu_onSwitchDaylightCycleType(boolean loadNewCycleConfig, Runnable consumer)
    {
        // If the main menu has more than 4 assigned elements, do the thing
        final var elements = this.menuElements.get(Menu.MAIN_MENU);
        final var areElementsNotEmpty = elements != null && !elements.isEmpty();

        if (areElementsNotEmpty)
        {
            // Clear the rest besides the default menu elements
            this.mainMenu_removeQuickOptionElements(elements);
        }

        Class<?> previousCycleBuilderClass;
        Class<?> currentCycleBuilderClass;

        var theresTimeChanger = TimeChangerStruggleClient.getTimeChanger() != null;
        previousCycleBuilderClass = theresTimeChanger ? TimeChangerStruggleClient.getTimeChanger().getBuilderClass() : null;

        // Runnable (or consumer) that actually changes the cycle type being used.
        // To see what this does, see the methods calling this method for it.
        consumer.run();

        theresTimeChanger = TimeChangerStruggleClient.getTimeChanger() != null;
        currentCycleBuilderClass = theresTimeChanger ? TimeChangerStruggleClient.getTimeChanger().getBuilderClass() : null;

        if (theresTimeChanger && previousCycleBuilderClass != currentCycleBuilderClass && loadNewCycleConfig)
        {
            TimeChangerStruggleClient.config.createOrModifyDaylightCycleConfig(TimeChangerStruggleClient.getTimeChanger(), false);
        }

        if (areElementsNotEmpty)
        {
            // Then create more elements for the main menu should there be any
            this.mainMenu_createQuickOptionElements(elements);

            // Lastly, update existing menu elements that has the previous daylight cycle name to
            // use the current daylight cycle name
            final var firstLineText = this.mainMenu_getButtonWidgetTooltipFirstLine_switchGetterMenu();

            // Switch Cycle Menu
            var bwe = (ButtonWidgetEx)elements.get(3);
            bwe.setMessage(this.mainMenu_getButtonWidgetText_switchGetterMenu());

            bwe.updateTooltip(firstLineText, Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.desc"), this.font);

            // Quick-Switch Cycle
            bwe = (ButtonWidgetEx)elements.get(4);

            bwe.updateTooltip(firstLineText, Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.quick.desc"), this.font);

            if (this.currentMenu == Menu.MAIN_MENU)
            {
                this.clearWidgets();
                this.initSelf();
            }
        }

        this.menuDirty = true;
    }

    void mainMenu_createQuickOptionElements(List<GuiEventListener> elements)
    {
        if (TimeChangerStruggleClient.getTimeChanger() == null)
        {
            return;
        }

        var quickOptionElements = TimeChangerStruggleClient.getTimeChanger().createQuickOptionElements(this);

        // First check if the current cycle supports any quick-option elements
        // Otherwise do nothing to add them into the main menu
        if (quickOptionElements == null || quickOptionElements.length <= 0)
        {
            return;
        }

        for (GuiEventListener elem : quickOptionElements)
        {
            if (elem == null)
            {
                continue;
            }

            if (elem instanceof WidgetConfigInterface)
            {
                ((WidgetConfigInterface<?, ?>)elem).getProperty().consumerOnlyIfNotExists(this);
            }

            elements.add(elem);
        }
    }

    void mainMenu_removeQuickOptionElements(List<GuiEventListener> elements)
    {
        if (elements != null && elements.size() > 6)
        {
            for (var i = elements.size() - 1; i >= 6; --i)
            {
                elements.remove(i);
            }
        }
    }

    private void mainMenu_saveQuickOptionElements()
    {
        if (this.mainMenu_saveCurrentOption != null && this.mainMenu_saveCurrentOption)
        {
            TimeChangerStruggleClient.config
                    .createOrModifyDaylightCycleConfig(TimeChangerStruggleClient.getTimeChanger(), true);

            this.mainMenu_saveCurrentOption = null;
        }
    }

    //
    // Switch Getter Menu and Getter Config Methods
    //
    private void switchDaylightCycleMenu_buildConfigFromType(DayNightCycleBasis cycleType, List<GuiEventListener> cycleMenu, boolean fromSelfInit)
    {
        var warrantAnUpdate = false;

        var existingCyclePropertyList = this.switchDaylightCycleMenu_getPropertyListFunc.apply(cycleMenu);

        if (existingCyclePropertyList != null)
        {
            this.switchDaylightCycleMenu_savePropertiesToConfig();

            cycleMenu.remove(existingCyclePropertyList);
            warrantAnUpdate = true;
        }

        if (cycleType == null)
        {
            if (warrantAnUpdate && !fromSelfInit)
            {
                this.clearWidgets();
                this.initSelf();
            }

            return;
        }

        var timeChangerDoesNotExist = TimeChangerStruggleClient.getTimeChanger() == null;

        // Don't forget to load its associated configs if attempting to do so unless if
        // there is an existing cycle loaded on memory which matches the current modifying
        // cycle as we do not want to load the config on the same exact menu
        if (timeChangerDoesNotExist || !cycleType.getBuilderClass().equals(TimeChangerStruggleClient.getTimeChanger().getBuilderClass()))
        {
            TimeChangerStruggleClient.config.createOrModifyDaylightCycleConfig(cycleType, false);
        }

        existingCyclePropertyList = new SwitchDaylightCyclePropertyList(this, cycleType);

        // Before adding, check if the property list actually has any property lists to make
        // use of, otherwise it is assumed that there were no useful properties
        if (existingCyclePropertyList.hasAnyElements())
        {
            cycleMenu.add(existingCyclePropertyList);
        }

        if (!fromSelfInit)
        {
            this.clearWidgets();
            this.initSelf();
        }
    }

    private void switchDaylightCycleMenu_savePropertiesToConfig()
    {
        var propList = this.switchDaylightCycleMenu_getPropertyList();

        if (propList == null)
        {
            return;
        }

        var timeChangerExists = TimeChangerStruggleClient.getTimeChanger() == null;

        if (propList.dirty && propList.hasModifiedProperties())
        {
            if (timeChangerExists && propList.modifyingCycleType.getBuilderClass().equals(TimeChangerStruggleClient.getTimeChanger().getBuilderClass()))
            {
                this.mainMenu_saveCurrentOption = null;
            }

            TimeChangerStruggleClient.config
                    .createOrModifyDaylightCycleConfig(propList.modifyingCycleType, true);
        }
    }

    private SwitchDaylightCyclePropertyList switchDaylightCycleMenu_getPropertyList()
    {
        // Fixes a crash that occurs when the Switch Daylight Cycle Menu is opened
        if (this.menuElements.containsKey(Menu.SWITCH_DAYLIGHT_CYCLE_MENU))
        {
            var cycleMenu = this.menuElements.get(Menu.SWITCH_DAYLIGHT_CYCLE_MENU);
            return this.switchDaylightCycleMenu_getPropertyListFunc.apply(cycleMenu);
        }

        return null;
    }

    private boolean switchDaylightCycleMenu_isCycleBuilderInMenu(DayNightCycleBuilder builderToCheck)
    {
        var propList = this.switchDaylightCycleMenu_getPropertyList();
        return propList == null ? false : propList.modifyingCycleType.getBuilderClass().equals(builderToCheck.getClass());
    }

    private MutableComponent switchDaylightCycleMenu_switchSoloCycleList_getTooltipFirstLine()
    {
        final var canRenderTwoLists = this.switchDaylightCycleMenu_canRenderTwoLists.getAsBoolean();

        var useSwitchText = this.switchDaylightCycleMenu_onlyPropertiesList == null ? canRenderTwoLists : this.switchDaylightCycleMenu_onlyPropertiesList;

        var formattedTextSupport = useSwitchText ? Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.daylightcycleswitch") : Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.daylightcycleproperties");

        return Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.switchsololist.desc.switchto", formattedTextSupport);
    }

    private MutableComponent switchDaylightCycleMenu_switchDualListVisibility_getTooltipFirstLine()
    {
        var useSingularText = this.switchDaylightCycleMenu_onlyPropertiesList == null;

        // Note: if useSinglularText is true, that simply means that it is by default showing multiple
        // lists (since this button won't be shown if the screen resolution is not enough to support
        // both) and instead returns a singular as in this context we are talking about "Switch to"
        var formattedTextSupport = useSingularText ? Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.switchduallist.single") : Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.switchduallist.dual");

        return Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.switchduallist.desc.switchto", formattedTextSupport);
    }

    private void switchDaylightCycleMenu_propertyList_saveProperties(Button b)
    {
        this.switchDaylightCycleMenu_propertyList_forEachProperty((modifyingCycleType, propElem) ->
        {
            modifyingCycleType.writePropertyValueToCycle(propElem.getProperty());
        });
    }

    private void switchDaylightCycleMenu_propertyList_resetProperties(Button b)
    {
        this.switchDaylightCycleMenu_propertyList_forEachProperty((modifyingCycleType, propElem) ->
        {
            propElem.forceSetWidgetValueToDefault(true);
            propElem.setPropertyValueToDefault(true);

            modifyingCycleType.writePropertyValueToCycle(propElem.getProperty());
        });
    }

    private void switchDaylightCycleMenu_propertyList_forEachProperty(BiConsumer<DayNightCycleBasis, WidgetConfigInterface<?, ?>> consumer)
    {
        var propList = this.switchDaylightCycleMenu_getPropertyList();

        if (propList != null && propList.sections != null)
        {
            propList.sections.stream()
                    .filter(section -> section.sectionChildrenEntries != null)
                    .forEachOrdered(section ->
                    {
                        section.sectionChildrenEntries.stream()
                                .filter(assumedProp -> assumedProp.properties != null)
                                .forEachOrdered(entry ->
                                {
                                    entry.properties.forEach(prop ->
                                    {
                                        consumer.accept(propList.modifyingCycleType, prop);
                                    });
                                });
                    });

            propList.dirty = true;
        }

        this.menuDirty = true;
    }

    private void toggleCycleMenuListVisibility(CycleButton<Boolean> b, boolean newValue)
    {
        // Ensure that the properties list exists
        if (b.active && this.switchDaylightCycleMenu_propertiesListExists.getAsBoolean())
        {
            final var canRenderTwoLists = this.switchDaylightCycleMenu_canRenderTwoLists.getAsBoolean();
            final var onlyPropsListNull = this.switchDaylightCycleMenu_onlyPropertiesList == null;

            var update = false;

            if (onlyPropsListNull && !canRenderTwoLists)
            {
                this.switchDaylightCycleMenu_onlyPropertiesList = true;
                b.setValue(true);

                update = true;
            }

            // if onlyPropertiesList isn't null then it is assumed that we are rendering only a list
            else if (!onlyPropsListNull)
            {
                this.switchDaylightCycleMenu_onlyPropertiesList = newValue;
                update = true;
            }

            if (update)
            {
                this.clearWidgets();
                this.initSelf();
            }
        }
    }

    private void toggleCycleMenuDualList(CycleButton<Boolean> b, boolean useDualList)
    {
        if (b.visible)
        {
            final var propertiesListEmpty = this.switchDaylightCycleMenu_onlyPropertiesList == null;

            if (useDualList)
            {
                if (!propertiesListEmpty)
                {
                    this.switchDaylightCycleMenu_onlyPropertiesList = null;

                    this.clearWidgets();
                    this.initSelf();
                }
            }
            else if (propertiesListEmpty)
            {
                @SuppressWarnings("unchecked")
                var cycleMenuListWidget = (CycleButton<Boolean>)this.menuElements.get(Menu.SWITCH_DAYLIGHT_CYCLE_MENU).get(1);

                this.switchDaylightCycleMenu_onlyPropertiesList = cycleMenuListWidget.getValue();

                this.clearWidgets();
                this.initSelf();
            }
        }
    }

    private void toggleCyclePropertyListAutoApply(CycleButton<Boolean> b, boolean shouldAutoApply)
    {
        TimeChangerStruggleClient.applyOnPropertyListValueUpdate = shouldAutoApply;

        b.setMessage(Component.nullToEmpty(shouldAutoApply ? "\u25C9" : "\u25EF"));

        var myIndex = this.children().indexOf(b);

        if (myIndex > -1)
        {
            // Save Properties
            var bwe = (ButtonWidgetEx)this.children().get(myIndex + 1);
            bwe.active = !shouldAutoApply;
            // Reset Properties to Previous State
            bwe = (ButtonWidgetEx)this.children().get(myIndex + 2);
            bwe.active = !shouldAutoApply;
        }
    }

    //
    // Static Methods
    //
    public static CyclingButtonWidgetEx<Boolean> createCyclingWidget(int w, int h, Component displayText, boolean startingValue, CycleButton.OnValueChange<Boolean> updateCallback, OptionInstance.TooltipSupplier<Boolean> tooltipFactory)
    {
        return TimeChangerScreen.createCyclingWidget(w, h, displayText, startingValue, false, CommonComponents.OPTION_ON, CommonComponents.OPTION_OFF, updateCallback, tooltipFactory);
    }

    public static CyclingButtonWidgetEx<Boolean> createCyclingWidget(int w, int h, Component displayText, boolean startingValue, boolean omitKeyText, Component onText, Component offText, CycleButton.OnValueChange<Boolean> updateCallback, OptionInstance.TooltipSupplier<Boolean> tooltipFactory)
    {
        var cb = CyclingButtonWidgetEx.booleanCycle(startingValue, onText, offText);

        if (tooltipFactory != null)
        {
            cb.withTooltip(tooltipFactory);
        }

        if (omitKeyText)
        {
            cb.displayOnlyValue();
        }

        return cb.build(w, h, displayText, updateCallback);
    }

    public static Tooltip createTooltips(Font textRenderer, byte useCase, Component onText, Component offText)
    {
        Component text = null;

        switch (useCase)
        {
            case 0:
            case 1:
            {
                text = useCase == 0 ? onText : offText;
                break;
            }
            case 2:
            {
                text = offText;
                break;
            }
        }

        if (useCase == 3)
        {
            return Tooltip.create(onText);
        }
        else if (text == null)
        {
            return Tooltip.create(Component.empty());
        }
        else
        {
            return Tooltip.create(Component.empty().append(useCase == 2 ? onText : Component.translatable("jugglestruggle.tcs.screen.desc")).append(" ").append(text)); // I don't think you can create multiline tooltips, so this is the closest I'm getting.
        }
    }

    /**
     *
     * @param textRenderer the text renderer used to align and separate lines that go over the hardcoded width
     * @param useCase <li> 0 = assumes as true, <li> 1 = assumes as false,
     * <li> 2 = assumes that the onText parameter is the first line while offText is the description,
     * <li> 3 = assumes the same as 2 but without offText requirement (and offText is not used)
     * @param onText the text to be used if useCase is 0; if useCase is 2 then it is used as the first-line description
     * @param offText the text to be used if useCase is 1; if useCase is 2 then it is used as the tooltip description
     *
     * @return a list of an ordered text for reasons unknown.
     */
    public static List<FormattedCharSequence> createOrderedTooltips(Font textRenderer, byte useCase, Component onText, Component offText)
    {
        Component text = null;

        switch (useCase)
        {
            case 0:
            case 1:
            {
                text = useCase == 0 ? onText : offText;
                break;
            }
            case 2:
            {
                text = offText;
                break;
            }
        }

        ImmutableList.Builder<FormattedCharSequence> wrappedTextBuilder;

        if (useCase == 3)
        {
            wrappedTextBuilder = ImmutableList.builderWithExpectedSize(1);
            wrappedTextBuilder.add(onText.getVisualOrderText());
        }
        else if (text == null)
        {
            return ImmutableList.of();
        }
        else
        {
            var wrappedText = textRenderer.split(text, 200);
            wrappedTextBuilder = ImmutableList.builderWithExpectedSize(wrappedText.size() + 1);

            if (useCase == 2)
            {
                wrappedTextBuilder.add(onText.getVisualOrderText());
            }
            else
            {
                wrappedTextBuilder.add(Component.translatable("jugglestruggle.tcs.screen.desc").getVisualOrderText());
            }

            wrappedTextBuilder.addAll(wrappedText);
        }

        return wrappedTextBuilder.build();
    }

    private static <PE extends ContainerEventHandler> GuiEventListener getHoveringElementWithPredicate(PE parent, int mouseX, int mouseY, Predicate<GuiEventListener> predicate)
    {
        for (GuiEventListener possibleHoveringElement : parent.children())
        {
            if (possibleHoveringElement.isMouseOver(mouseX, mouseY) && (predicate == null || predicate.test(possibleHoveringElement)))
            {
                return possibleHoveringElement;
            }
        }

        return null;
    }

    public static int[] getTooltipForWidgetWidthHeight(final List<FormattedCharSequence> tooltipText, Font textRenderer)
    {
        var totalTooltipTextWidth = 2;
        var totalTooltipTextHeight = textRenderer.lineHeight * tooltipText.size();

        for (FormattedCharSequence text : tooltipText)
        {
            var textWidth = textRenderer.width(text);

            if (textWidth > totalTooltipTextWidth)
            {
                totalTooltipTextWidth = textWidth;
            }
        }

        totalTooltipTextWidth += 4;

        return new int[] { totalTooltipTextWidth, totalTooltipTextHeight };
    }

    private static void renderText(GuiGraphics graphics, Font renderer, Component textToRender, float x, float y, int maxWidth, boolean center, int color)
    {
        if (textToRender == null)
        {
            return;
        }

        var trimmedText = renderer.plainSubstrByWidth(textToRender.getString(), maxWidth);

        if (center)
        {
            var trimmedTextWidth = renderer.width(trimmedText);
            x = x + maxWidth / 2 - trimmedTextWidth / 2;
        }

        graphics.drawString(renderer, trimmedText, (int)x, (int)y + 2, color);
    }

    /**
     * Makes sure that this particular element is unfocused.
     *
     * @param elementToDefocus the element to unfocus
     * @param <E> the element type to defocus
     */
    private static <E extends GuiEventListener> void defocusElement(E elementToDefocus)
    {
        if (elementToDefocus.isFocused())
        {
            elementToDefocus.setFocused(false);
        }
    }

    static void renderTooltips(GuiGraphics graphics, TimeChangerScreen parent, ContainerEventHandler sourceElement, int mouseX, int mouseY, int offsetX, int offsetY)
    {
        var obtainedElement = TimeChangerScreen.getHoveringElementWithPredicate(sourceElement, mouseX, mouseY, TimeChangerScreen.ORDERABLE_TOOLTIP_PREDICATE);

        if (obtainedElement == null || !(obtainedElement instanceof PositionedTooltip))
        {
            return;
        }

        var tooltipText = ((PositionedTooltip)obtainedElement).getOrderedTooltip();

        if (tooltipText == null)
        {
            return;
        }

        int x;
        int y;

        // There really has to be a better way of doing this without creating some
        // sort of hack'n slash just to render the tooltip on top of the element
        if (obtainedElement instanceof AbstractWidget cw)
        {
            final var offsetPos = TimeChangerScreen.getTooltipForWidgetWidthHeight(tooltipText, parent.font);

            x = cw.getX() + offsetX + cw.getWidth() / 2 - offsetPos[0] / 2 - 10;
            y = cw.getY() + offsetY - offsetPos[1];

            if (x + offsetPos[0] + 10 > parent.width)
            {
                x = parent.width - offsetPos[0] - 15;
            }
            else if (x < -5)
            {
                x = -5;
            }
            if (y + offsetPos[1] > parent.height)
            {
                y = parent.height - offsetPos[1] - 10;
            }
            else if (y < 0)
            {
                y = 0;
            }
        }
        else
        {
            x = mouseX;
            y = mouseY;
        }

        graphics.renderTooltip(parent.font, tooltipText, x, y);
    }

    //
    // Classes: Menu-Specific (Switch Getter Menu)
    //
    private class SwitchGetterBuilderList extends SwitchGetterBasisBuilderList<SwitchDaylightCycleBuilderListEntry>
    {
        public SwitchGetterBuilderList(TimeChangerScreen parent)
        {
            super(parent, 24);
        }

        public final void addSectionEntries()
        {
            TimeChangerStruggleClient.getCachedCycleTypeBuilders().stream().forEach(builder ->
            {
                this.addEntry(new SwitchDaylightCycleBuilderListEntry(this, builder));
            });

            this.title = Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.daylightcycleswitch.title", TimeChangerStruggleClient.getCachedCycleTypeSize());
        }
    }

    private class SwitchDaylightCyclePropertyList extends SwitchGetterBasisBuilderList<DaylightCyclePropertyListEntry>
    {
        public boolean dirty;
        final DayNightCycleBasis modifyingCycleType;
        final List<DaylightCyclePropertyListEntry> sections;

        public SwitchDaylightCyclePropertyList(TimeChangerScreen parent, DayNightCycleBasis modifyingCycleType)
        {
            super(parent, 24);

            this.modifyingCycleType = modifyingCycleType;

            var props = this.modifyingCycleType.createProperties();

            if (props == null || props.isEmpty())
            {
                this.sections = null;
            }
            else
            {
                var section = FancySectionProperty.EMPTY;

                // Cache the section and the list of properties gathered here
                Map<FancySectionProperty, List<WidgetConfigInterface<?, ?>>> cachedSectionConfigs = new LinkedHashMap<>(props.size());

                for (BaseProperty<?, ?> prop : props)
                {
                    // Sets the "section" to the current section from the property and
                    // go for the next property
                    if (prop instanceof FancySectionProperty)
                    {
                        section = (FancySectionProperty)prop;
                        continue;
                    }

                    // Create a config element based on the property type
                    WidgetConfigInterface<?, ?> configElement = prop.createConfigElement(parent, section);

                    // If none, skip this property
                    if (configElement == null)
                    {
                        continue;
                    }

                    // Assuming that Java references objects, including inside of the created element,
                    // then just use the current property instead of the widget's property to create
                    // a "hey, call me instead"
                    prop.consumerOnlyIfNotExists(this.parent);

                    // Check if a section exists; otherwise create a list then save it
                    // in the map
                    var sectionExists = cachedSectionConfigs.containsKey(section);

                    var sectionElements = sectionExists ? cachedSectionConfigs.get(section) : new ArrayList<WidgetConfigInterface<?, ?>>();

                    sectionElements.add(configElement);

                    if (!sectionExists)
                    {
                        cachedSectionConfigs.put(section, sectionElements);
                    }
                }

                var sectionsToCache = ImmutableList.<DaylightCyclePropertyListEntry>builderWithExpectedSize(cachedSectionConfigs.size());

                for (var entry : cachedSectionConfigs.entrySet())
                {
                    // Allow the modifying cycle type to decide how to rearrange the elements given from
                    // the created properties it made
                    var orderedElements = this.modifyingCycleType.rearrangeSectionElements(entry, 2);

                    // If our rearranged elements return empty (mostly it shouldn't but it can happen) then
                    // skip this entry as we are not going to add sections without children entries
                    if (orderedElements == null || orderedElements.length <= 0)
                    {
                        continue;
                    }

                    section = entry.getKey();

                    // Create an immutable set to cache before the section gets the finalized
                    // set and to add the entries after the section
                    var properties = ImmutableSet.<DaylightCyclePropertyListEntry>builderWithExpectedSize(orderedElements.length);

                    // Add the rows and its associated properties
                    for (WidgetConfigInterface<?, ?>[] elements : orderedElements)
                    {
                        properties.add(new DaylightCyclePropertyListEntry(this, section, elements));
                    }

                    // Build the sets and use it to be added into the owning section including
                    // to the entries
                    Set<DaylightCyclePropertyListEntry> finalizedProps = properties.build();

                    // First add the section before the entries
                    if (section != null && !section.property().isBlank() && section.get() != null)
                    {
                        var sectionEntry = new DaylightCyclePropertyListEntry(this, section, finalizedProps);

                        sectionsToCache.add(sectionEntry);
                        this.addEntry(sectionEntry);
                    }

                    // Then add the entries!
                    finalizedProps.stream().forEach(propEntry ->
                    {
                        this.addEntry(propEntry);
                    });
                }

                this.sections = sectionsToCache.build();

                var cycleBuilder = TimeChangerStruggleClient
                        .getCachedCycleBuilderByClass(this.modifyingCycleType.getBuilderClass());

                this.title = Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.daylightcycleproperties.title", cycleBuilder.isPresent() ? cycleBuilder.get().getTranslatableName() : Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.desc.using.none"));
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button)
        {
            final var clicked = super.mouseClicked(mouseX, mouseY, button);

            if (clicked)
            {
                var entryFocused = super.getFocused();

                if (entryFocused != null)
                {
                    super.children().stream()
                            .filter(propEntry -> (propEntry != entryFocused))
                            .forEach(propEntry ->
                            {
                                propEntry.defocusChildrenElements();
                            });
                }
            }

            return clicked;

        }

        public boolean hasAnyElements()
        {
            return !super.children().isEmpty();
        }

        @Override
        public void onLocSizeUpdate()
        {
            super.children().stream().forEach(propEntry ->
            {
                propEntry.onLocSizeUpdate();
            });
        }

        public boolean hasModifiedProperties()
        {
            for (DaylightCyclePropertyListEntry propEntry : super.children())
            {
                if (propEntry.isSection)
                {
                    continue;
                }

                for (WidgetConfigInterface<?, ?> prop : propEntry.properties)
                {
                    if (!prop.isDefaultValue())
                    {
                        return true;
                    }
                }
            }

            return false;
        }

        public void updateChildren()
        {
            super.clearEntries();
            super.setSelected(null);

            if (this.sections == null || this.sections.isEmpty())
            {
                return;
            }

            this.sections.forEach(section ->
            {
                this.addEntry(section);

                if (section.sectionShowButton.getValue())
                {
                    section.sectionChildrenEntries.forEach(prop ->
                    {
                        this.addEntry(prop);
                    });
                }
            });

            this.onLocSizeUpdate();
        }
    }

    private static class SwitchDaylightCycleBuilderListEntry extends
            SwitchGetterBasisBuilderEntry<SwitchDaylightCycleBuilderListEntry>
    {
        public final DayNightCycleBuilder builder;

        private final Component name;
        private final Component description;

        private final ButtonWidgetEx options;
        private final ButtonWidgetEx createAndUse;

        private final SwitchGetterBuilderList parent;

        /** Signifies that this entry is currently in use in terms of the cycle type. */
        public boolean inUse;
        /** Signifies if this option is being edited on a property list. */
        public boolean onEdit;

        public SwitchDaylightCycleBuilderListEntry(SwitchGetterBuilderList parent, DayNightCycleBuilder builder)
        {
            this.builder = builder;
            this.parent = parent;

            this.name = this.builder.getTranslatableName();
            this.description = this.builder.getTranslatableDescription();

            this.options = new ButtonWidgetEx(20, 20,

                    Component.nullToEmpty("\u26A1"),

                    Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.cycleentry.useasoption", this.name),
                    /* Text.translatable("jugglestruggle.tcs.screen.desc") */ null, parent.parent.font,

                    this::onCreateOptionClick);
            this.createAndUse = new ButtonWidgetEx(20, 20,

                    Component.nullToEmpty("\u2192"),

                    Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.cycleentry.use", this.name),
                    /* Text.translatable("jugglestruggle.tcs.screen.desc") */ null, parent.parent.font,

                    this::onCreateAndUseClick);

            if (this.builder.hasOptionsToEdit())
            {
                var propList = this.parent.parent.switchDaylightCycleMenu_getPropertyList();

                if (propList == null)
                {
                    this.options.active = true;
                }
                else
                {
                    this.setOnEdit(propList.modifyingCycleType
                            .getBuilderClass().equals(this.builder.getClass()));
                }

            }
            else
            {
                this.options.active = false;
            }

            this.setSelectedIfTimeChangerMatches();
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            return ImmutableList.of(this.options, this.createAndUse);
        }

        @Override
        public List<? extends NarratableEntry> narratables()
        {
            if (this.options.active && this.createAndUse.active)
            {
                return ImmutableList.of(this.options, this.createAndUse);
            }
            else if (this.options.active)
            {
                return ImmutableList.of(this.options);
            }
            else if (this.createAndUse.active)
            {
                return ImmutableList.of(this.createAndUse);
            }
            else
            {
                return ImmutableList.of();
            }
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta)
        {
            this.options.setX(entryWidth - 35);
            this.options.setY(y + 2);
            this.createAndUse.setX(this.options.getX() + 22);
            this.createAndUse.setY(this.options.getY());

            int colorStart;
            int colorEnd;
            int colorTextName;
            int colorTextDesc;

            if (hovered)
            {
                //                colorStart = 0xFFFFAADD; colorEnd = 0x66AADDFF; colorTextName = -1; colorTextDesc = 0xFF552244;
                colorStart = 0xFF010072;
                colorEnd = 0xFF0f98E3;
                colorTextName = -1;
                colorTextDesc = 0xFFAAFFFF;
            }
            else if (this.inUse)
            {
                colorStart = 0xFFAADD00;
                colorEnd = 0xFF00AADD;
                colorTextName = 0;
                colorTextDesc = 0xFFCC2222;
            }
            else if (this.onEdit)
            {
                colorStart = 0xFFCC8800;
                colorEnd = 0xFFCCCC00;
                colorTextName = 0xFF4444FF;
                colorTextDesc = 0xFF222222;
            }
            else
            {
                colorStart = 0xA0000000;
                colorEnd = 0x44000000;
                colorTextName = -1;
                colorTextDesc = 0xFFAAAAAA;
            }

            graphics.fillGradient(x, y, x + entryWidth, y + entryHeight + 4, colorStart, colorEnd, 0);

            var maxRenderTextWidth = entryWidth - 56;

            final var textRenderer = this.parent.parent.font;

            TimeChangerScreen.renderText(graphics, textRenderer, this.name, x + 4, y + 2, maxRenderTextWidth, false, colorTextName);
            TimeChangerScreen.renderText(graphics, textRenderer, this.description, x + 4, y + textRenderer.lineHeight + 2, maxRenderTextWidth, false, colorTextDesc);

            this.options.render(graphics, mouseX, mouseY, tickDelta);
            this.createAndUse.render(graphics, mouseX, mouseY, tickDelta);
        }

        private void setSelectedIfTimeChangerMatches()
        {
            this.setInUse(this.isDaylightCycleTypeEqual());
        }

        private boolean isDaylightCycleTypeEqual()
        {
            return TimeChangerStruggleClient.getTimeChanger() != null && this.builder.getClass().equals(TimeChangerStruggleClient.getTimeChanger().getBuilderClass());
        }

        private void setInUse(boolean inUse)
        {
            this.inUse = inUse;
            this.createAndUse.active = !this.inUse;
        }

        private void setOnEdit(boolean onEdit)
        {
            this.onEdit = onEdit;
            this.options.active = !this.onEdit && this.builder.hasOptionsToEdit();
        }

        private void onCreateAndUseClick(Button widget)
        {
            // Firstly, check if this builder is already in use before attempting to
            // make changes to certain elements
            if (this.isDaylightCycleTypeEqual())
            {
                return;
            }

            final var myInstance = this;

            // Gather all of the parent's child entries and set their selected status to
            // false to avoid having multiple items be "in use" when that is not the case
            this.parent.children().stream()
                    .filter(sg -> (sg.inUse && sg != myInstance))
                    .forEach(sg ->
                    {
                        sg.setInUse(false);
                    });

            // Do be sure that if we are going to load the config, it is only loading them
            // if there are no properties list loaded for that particular type or if it is,
            // then just use our existing cycle type rather than creating a new one; no reason
            // to be wasting memory on something that already exists
            final var propList = this.parent.parent.switchDaylightCycleMenu_getPropertyList();
            final var isExistingCycleType = this.parent.parent.switchDaylightCycleMenu_isCycleBuilderInMenu(this.builder);
            final var loadNewConfig = propList == null || !isExistingCycleType;

            this.parent.parent.mainMenu_onSwitchDaylightCycleType(loadNewConfig, () ->
            {
                TimeChangerStruggleClient.setTimeChanger(isExistingCycleType ? propList.modifyingCycleType : this.builder.create());
            });

            this.setSelectedIfTimeChangerMatches();
        }

        private void onCreateOptionClick(Button widget)
        {
            if (this.parent.parent.switchDaylightCycleMenu_isCycleBuilderInMenu(this.builder))
            {
                return;
            }

            DayNightCycleBasis cycleType;

            if (this.isDaylightCycleTypeEqual())
            {
                cycleType = TimeChangerStruggleClient.getTimeChanger();
            }
            else
            {
                cycleType = this.builder.create();
            }

            var cycleMenu = this.parent.parent.menuElements.get(Menu.SWITCH_DAYLIGHT_CYCLE_MENU);
            this.parent.parent.switchDaylightCycleMenu_buildConfigFromType(cycleType, cycleMenu, false);

            final var myInstance = this;

            this.parent.children().stream()
                    .filter(sg -> (sg.onEdit && sg != myInstance))
                    .forEach(sg ->
                    {
                        sg.setOnEdit(false);
                    });

            var propListElement = this.parent.parent.switchDaylightCycleMenu_getPropertyList();

            this.setOnEdit(propListElement == null ? false : propListElement.modifyingCycleType == cycleType);
        }
    }

    private static class DaylightCyclePropertyListEntry extends
            SwitchGetterBasisBuilderEntry<DaylightCyclePropertyListEntry>
    {
        private final SwitchDaylightCyclePropertyList parent;

        public final FancySectionProperty owningSection;
        public final boolean isSection;

        // Section-Exclusive
        final CyclingButtonWidgetEx<Boolean> sectionShowButton;
        final Set<DaylightCyclePropertyListEntry> sectionChildrenEntries;

        // Properties-Exclusive
        final List<WidgetConfigInterface<?, ?>> properties;

        // Other things
        boolean updateLocSizeForElements;

        // Section Entry
        public DaylightCyclePropertyListEntry(SwitchDaylightCyclePropertyList parent,
                FancySectionProperty prop, Set<DaylightCyclePropertyListEntry> childrenEntries)
        {
            this.parent = parent;
            this.owningSection = prop;
            this.isSection = true;

            this.sectionChildrenEntries = childrenEntries;
            this.properties = null;

            var builder = CyclingButtonWidgetEx.booleanCycle(true, null, null);

            builder.withInitialValue(true);
            builder.displayOnlyValue();
            builder.withTooltip(this::onShowHidePropertiesButtonApplyTooltip);

            this.sectionShowButton = builder.build(20, 20, Component.empty(), this::onShowHidePropertiesButtonUpdate);
            this.sectionShowButton.setMessage(Component.nullToEmpty("\u2191"));
        }

        // Property Entry
        public DaylightCyclePropertyListEntry(SwitchDaylightCyclePropertyList parent,
                FancySectionProperty prop, WidgetConfigInterface<?, ?>[] rowElements)
        {
            this.parent = parent;
            this.owningSection = prop;
            this.isSection = false;

            this.sectionChildrenEntries = null;
            this.sectionShowButton = null;

            ImmutableList.Builder<WidgetConfigInterface<?, ?>> elements = ImmutableList.builder();

            for (WidgetConfigInterface<?, ?> element : rowElements)
            {
                if (element == null || element.getProperty() == null)
                {
                    continue;
                }

                elements.add(element);
            }

            this.properties = elements.build();
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            return this.isSection ? ImmutableList.of(this.sectionShowButton) : this.properties;
        }

        @Override
        public List<? extends NarratableEntry> narratables()
        {
            if (this.isSection)
            {
                return this.sectionShowButton.visible ? ImmutableList.of(this.sectionShowButton) : ImmutableList.of();
            }
            else
            {
                return this.properties;
            }
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta)
        {
            if (this.updateLocSizeForElements)
            {
                this.onLocSizeUpdateFromRender(x, y, entryWidth, entryHeight);
                this.updateLocSizeForElements = false;
            }

            int colorStart;
            int colorEnd;
            int colorTextName;
            //            int colorTextDesc;

            if (this.isSection)
            {
                if (hovered)
                {
                    colorStart = 0xFF0f98E3;
                    colorEnd = 0xFF010072;
                    colorTextName = -1;
                }
                else
                {
                    colorStart = 0xFF5FA6E6;
                    colorEnd = 0xFF00153E;
                    colorTextName = -1;
                }
            }
            else
            {
                colorStart = 0xFF000000;
                colorEnd = 0x55000F0F;
                colorTextName = -1;
            }

            graphics.fillGradient(x, y, x + entryWidth, y + entryHeight + 4, colorStart, colorEnd, 0);

            var maxRenderTextWidth = entryWidth - 30;

            final var textRenderer = this.parent.parent.font;

            if (this.isSection)
            {
                var textWidth = textRenderer.width(this.owningSection.get().getVisualOrderText());
                TimeChangerScreen.renderText(graphics, textRenderer, this.owningSection.get(), x + entryWidth / 2 - textWidth / 2, y + entryHeight / 2 - textRenderer.lineHeight / 2, maxRenderTextWidth, false, colorTextName);

                this.sectionShowButton.render(graphics, mouseX, mouseY, tickDelta);
            }
            else
            {
                this.properties.forEach(elem -> elem.render(graphics, mouseX, mouseY, tickDelta));
            }
        }

        // Used from selfInit() in the main screen to avoid size and location updates
        // whenever one renders when they can instead update at the appropriate time
        public void onLocSizeUpdate()
        {
            this.updateLocSizeForElements = true;

        }

        private void onLocSizeUpdateFromRender(int x, int y, int entryWidth, int entryHeight)
        {
            if (this.isSection)
            {
                this.sectionShowButton.setX(x + entryWidth - 22);
                this.sectionShowButton.setY(y + 2);
            }
            else
            {
                this.parent.modifyingCycleType.rearrangeCreatedOptionElements(x, y, entryWidth, entryHeight, this.properties);
            }
        }

        public void defocusChildrenElements()
        {
            if (this.isSection)
            {
                TimeChangerScreen.defocusElement(this.sectionShowButton);
            }
            else
            {
                this.properties.forEach(TimeChangerScreen::defocusElement);
            }
        }

        //
        // Section-Exclusive Methods
        //
        private void onShowHidePropertiesButtonUpdate(CycleButton<Boolean> widget, Boolean shown)
        {
            this.parent.updateChildren();

            // Downward Arrow (Show) or Upward Arrow (Hide)
            widget.setMessage(Component.nullToEmpty(shown ? "\u2191" : "\u2193"));

            //            if (shown)
            //                widget.setMessage(Text.of("\u2191")); // Downward Arrow (Show)
            //            else
            //                widget.setMessage(Text.of("\u2193")); // Upward Arrow (Hide)
        }

        private Tooltip onShowHidePropertiesButtonApplyTooltip(Boolean shown)
        {
            return Tooltip.create(Component.translatable("jugglestruggle.tcs.screen.switchcyclemenu.propertylist." +
                    (shown ? "hide" : "show") + ".desc"));
            //            return TimeChangerScreen.createOrderedTooltips
            //            (
            //                this.parent.parent.getTextRenderer(), (byte)3, Component.translatable(
            //                    "jugglestruggle.tcs.screen.switchcyclemenu.propertylist."+
            //                    (shown ? "hide" : "show")+".desc"), null
            //            );
        }
    }

    private abstract class SwitchGetterBasisBuilderList<E extends SwitchGetterBasisBuilderEntry<E>> extends ContainerObjectSelectionList<E>
    {
        protected final TimeChangerScreen parent;
        protected boolean visible;
        protected Component title;
        private int bottom;

        public SwitchGetterBasisBuilderList(TimeChangerScreen parent, int itemSize)
        {
            super(parent.minecraft, 0, 0, 0, itemSize);

            this.parent = parent;
            this.visible = true;

            this.setRenderBackground(false);
            this.setRenderHeader(false, -4);

            this.centerListVertically = false;
        }

        public int getLeft()
        {
            return super.getX();
        }

        public int getRight()
        {
            return super.getRight();
        }

        public int getBottom()
        {
            return bottom;
        }

        public int getHeight()
        {
            return super.height;
        }

        public void setTopPos(int y)
        {
            this.setY(y);
            bottom = y + super.height;
        }

        public void setWidth(int w)
        {
            super.width = w;
            this.setX(this.getX());
        }

        public void setHeight(int h)
        {
            super.height = h;
            this.setTopPos(super.getY());
        }

        @Override
        public int getRowWidth()
        {
            return super.width - (this.getMaxScroll() > 0 ? 6 : 0);
        }

        @Override
        public int getRowLeft()
        {
            return super.getX();
        }

        @Override
        public int getRowRight()
        {
            return super.getRowRight();
        }

        @Override
        protected int getRowTop(int index)
        {
            return this.getY() - (int)this.getScrollAmount() + index * this.itemHeight;
        }

        @Override
        protected int getScrollbarPosition()
        {
            return super.getRight() - (this.getMaxScroll() > 0 ? 6 : 0);
        }

        public boolean isVisible()
        {
            return this.visible;
        }

        public void setVisible(boolean visible)
        {
            this.visible = visible;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY)
        {
            return this.visible ? super.isMouseOver(mouseX, mouseY) : false;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button)
        {
            return this.visible ? super.mouseClicked(mouseX, mouseY, button) : false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button)
        {
            return this.visible ? super.mouseReleased(mouseX, mouseY, button) : false;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
        {
            return this.visible ? super.mouseScrolled(mouseX, mouseY, scrollX, scrollY) : false;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers)
        {
            return this.visible ? super.keyPressed(keyCode, scanCode, modifiers) : false;
        }

        @Override
        public boolean keyReleased(int keyCode, int scanCode, int modifiers)
        {
            return this.visible ? super.keyReleased(keyCode, scanCode, modifiers) : false;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta)
        {
            if (!this.visible)
            {
                return;
            }

            graphics.fillGradient(this.getX(), this.getY(), this.getRight(), this.getBottom(), 0xAA334400, 0x55002233);

            if (this.title != null)
            {
                var y = this.getY() - this.parent.font.lineHeight - 6;
                graphics.fillGradient(this.getX(), y, this.getRight(), y + this.parent.font.lineHeight + 4, 0xAA000000, 0x77000000);
                TimeChangerScreen.renderText(graphics, this.parent.font, this.title, this.getX(), y + 1, this.width, true, -1);
            }

            final var scale = this.minecraft.getWindow().getGuiScale();
            // Has to conform to using OpenGL's way since it always starts bottom-left
            final var selfTop = this.parent.height - (this.getY() + this.height);

            RenderSystem.enableScissor((int)(this.getX() * scale), (int)(selfTop * scale), (int)(this.width * scale), (int)(this.height * scale));

            super.render(graphics, mouseX, mouseY, delta);

            RenderSystem.disableScissor();
        }

        @Override
        protected void renderItem(GuiGraphics graphics, int mouseX, int mouseY, float delta, int index, int x, int y, int entryWidth, int entryHeight)
        {
            super.renderItem(graphics, mouseX, mouseY, delta, index, x, y, entryWidth, entryHeight);
        }

        public void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY)
        {
            if (this.isMouseOver(mouseX, mouseY))
            {
                var entry = this.getEntryAtPosition(mouseX, mouseY);

                if (entry != null)
                {
                    TimeChangerScreen.renderTooltips(graphics, this.parent, entry, mouseX, mouseY, 0, 8);
                }
            }
        }

        // Don't just leave things as they are; also update children which
        // rely on location updates like property editing
        @Override
        public void setScrollAmount(double amount)
        {
            super.setScrollAmount(amount);
            this.onLocSizeUpdate();
        }

        @Override
        protected E getEntryAtPosition(double x, double y)
        {
            return super.getEntryAtPosition(x, y + 4);
        }

        public void onLocSizeUpdate()
        {}

        public void tick()
        {
            this.children().forEach(entry ->
            {
                entry.tick();
            });
        }
    }

    private abstract static class SwitchGetterBasisBuilderEntry<E extends SwitchGetterBasisBuilderEntry<E>> extends ContainerObjectSelectionList.Entry<E>
    {
        public void tick()
        {
            List<? extends GuiEventListener> children = this.children();

            if (children == null || children.isEmpty())
            {
                return;
            }
        }
    }

    //
    // Classes
    //
    private enum Menu
    {
        MAIN_MENU, CONFIGURATION_MENU, SWITCH_DAYLIGHT_CYCLE_MENU;

        public final boolean clearMenuOnSwitch()
        {
            return switch (this)
            {
                default -> false;
                case SWITCH_DAYLIGHT_CYCLE_MENU -> true;
            };
        }
    }
}
