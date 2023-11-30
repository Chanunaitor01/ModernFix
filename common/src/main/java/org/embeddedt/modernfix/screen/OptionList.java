package org.embeddedt.modernfix.screen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.embeddedt.modernfix.ModernFix;
import org.embeddedt.modernfix.core.ModernFixMixinPlugin;
import org.embeddedt.modernfix.core.config.Option;
import org.embeddedt.modernfix.core.config.OptionCategories;
import org.embeddedt.modernfix.platform.ModernFixPlatformHooks;

import java.io.IOException;
import java.util.*;

public class OptionList extends ContainerObjectSelectionList<OptionList.Entry> {
    private int maxNameWidth = 0;

    private static final int DEPTH_OFFSET = 20;

    private static final Component OPTION_ON = Component.translatable("modernfix.option.on").withStyle(style -> style.withColor(ChatFormatting.GREEN));
    private static final Component OPTION_OFF = Component.translatable("modernfix.option.off").withStyle(style -> style.withColor(ChatFormatting.RED));

    private static final Set<String> OPTIONS_MISSING_HELP = new HashSet<>();

    private ModernFixConfigScreen mainScreen;

    private static MutableComponent getOptionComponent(Option option) {
        String friendlyKey = "modernfix.option.name." + option.getName();
        MutableComponent baseComponent = Component.literal(option.getSelfName());
        if(I18n.exists(friendlyKey))
            return Component.translatable(friendlyKey).withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, baseComponent)));
        else
            return baseComponent;
    }

    public void updateOptionEntryStatuses() {
        for(Entry e : this.children()) {
            if(e instanceof OptionEntry) {
                ((OptionEntry)e).updateStatus();
            }
        }
    }

    private final Set<Option> addedOptions = new HashSet<>();

    private void addOption(Option option) {
        if(addedOptions.add(option)) {
            int w = this.minecraft.font.width(getOptionComponent(option)) + DEPTH_OFFSET * option.getDepth();
            this.maxNameWidth = Math.max(w, this.maxNameWidth);
            this.addEntry(new OptionEntry(option.getName(), option));
            ModernFixMixinPlugin.instance.config.getOptionMap().values().stream()
                    .filter(subOption -> subOption.getParent() == option)
                    .sorted(Comparator.comparing(Option::getName))
                    .forEach(this::addOption);
        }
    }

    public OptionList(ModernFixConfigScreen arg, Minecraft arg2) {
        super(arg2,arg.width + 45, arg.height - 52, 20, 20);

        this.mainScreen = arg;

        Multimap<String, Option> optionsByCategory = ModernFixMixinPlugin.instance.config.getOptionCategoryMap();
        List<String> theCategories = OptionCategories.getCategoriesInOrder();
        for(String category : theCategories) {
            String categoryTranslationKey = "modernfix.option.category." + category;
            this.addEntry(new CategoryEntry(Component.translatable(categoryTranslationKey)
                    .withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(categoryTranslationKey + ".description"))))
            ));
            optionsByCategory.get(category).stream().filter(key -> {
                int dotCount = 0;
                for(char c : key.getName().toCharArray()) {
                    if(c == '.')
                        dotCount++;
                }
                return dotCount >= 2;
            }).sorted(Comparator.comparing(Option::getName)).forEach(this::addOption);
        }
    }

    protected int getScrollbarPosition() {
        return super.getScrollbarPosition() + 15 + 20;
    }

    public int getRowWidth() {
        return super.getRowWidth() + 32;
    }

    class CategoryEntry extends Entry {
        private final Component name;
        private final int width;

        public CategoryEntry(Component component) {
            this.name = component;
            this.width = OptionList.this.minecraft.font.width(this.name);
        }

        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
            Font var10000 = OptionList.this.minecraft.font;
            float x = (float)(OptionList.this.minecraft.screen.width / 2 - this.width / 2);
            int y = top + height - 10;
            guiGraphics.drawString(var10000, this.name, (int)x, y, 16777215);
            /*
            if(mouseX >= x && mouseY >= y && mouseX <= (x + this.width) && mouseY <= (y + OptionList.this.minecraft.font.lineHeight))
                OptionList.this.mainScreen.renderComponentHoverEffect(matrixStack, this.name.getStyle(), mouseX, mouseY);

             */
        }

        public boolean changeFocus(boolean focus) {
            return false;
        }

        public List<? extends GuiEventListener> children() {
            return Collections.emptyList();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return Collections.emptyList();
        }
    }

    class OptionEntry extends Entry {
        private final String name;

        private final Button toggleButton;
        private final Button helpButton;
        private final Option option;

        public OptionEntry(String optionName, Option option) {
            this.name = optionName;
            this.option = option;
            Tooltip toggleTooltip = null;
            if(this.option.isModDefined()) {
                String disablingMods = String.join(", ", this.option.getDefiningMods());
                toggleTooltip = Tooltip.create(Component.translatable("modernfix.option." + (this.option.isEnabled() ? "enabled" : "disabled"))
                        .append(Component.translatable("modernfix.option.mod_override", disablingMods)));
            }
            this.toggleButton = new Button.Builder(Component.literal(""), (arg) -> {
                this.option.setEnabled(!this.option.isEnabled(), !this.option.isUserDefined());
                try {
                    ModernFixMixinPlugin.instance.config.save();
                    if(!OptionList.this.mainScreen.madeChanges) {
                        OptionList.this.mainScreen.madeChanges = true;
                    }
                } catch(IOException e) {
                    // revert
                    this.option.setEnabled(!this.option.isEnabled(), !this.option.isUserDefined());
                    ModernFix.LOGGER.error("Unable to save config", e);
                }
                OptionList.this.updateOptionEntryStatuses();
            }).tooltip(toggleTooltip).pos(0, 0).size(55, 20).build();
            updateStatus();
            this.helpButton = new Button.Builder(Component.literal("?"), (arg) -> {
                mainScreen.setLastScrollAmount(getScrollAmount());
                Minecraft.getInstance().setScreen(new ModernFixOptionInfoScreen(mainScreen, optionName));
            }).pos(75, 0).size(20, 20).build();
            if(!I18n.exists("modernfix.option." + optionName)) {
                this.helpButton.active = false;
                if(ModernFixPlatformHooks.INSTANCE.isDevEnv() && OPTIONS_MISSING_HELP.add(optionName))
                    ModernFix.LOGGER.warn("Missing help for {}", optionName);
            }
        }

        void updateStatus() {
            this.toggleButton.active = !(this.option.isModDefined() || this.option.isEffectivelyDisabledByParent());
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
            MutableComponent nameComponent = getOptionComponent(option);
            if(this.option.isUserDefined())
                nameComponent = nameComponent.withStyle(style -> style.withItalic(true)).append(Component.translatable("modernfix.config.not_default"));
            float textX = (float)(left + DEPTH_OFFSET * option.getDepth() + 160 - OptionList.this.maxNameWidth);
            float textY = (float)(top + height / 2 - 4);
            guiGraphics.drawString(OptionList.this.minecraft.font, nameComponent, (int)textX, (int)textY, 16777215);
            this.toggleButton.setPosition(left + 175, top);
            this.toggleButton.setMessage(getOptionMessage(this.option));
            this.toggleButton.render(guiGraphics, mouseX, mouseY, partialTicks);
            this.helpButton.setPosition(left + 175 + 55, top);
            this.helpButton.render(guiGraphics, mouseX, mouseY, partialTicks);
            /*
            if(mouseX >= textX && mouseY >= textY && mouseX <= (textX + OptionList.this.maxNameWidth) && mouseY <= (textY + OptionList.this.minecraft.font.lineHeight))
                OptionList.this.mainScreen.renderComponentHoverEffect(matrixStack, nameComponent.getStyle(), mouseX, mouseY);

             */
        }

        private Component getOptionMessage(Option option) {
            return option.isEnabled() ? OPTION_ON : OPTION_OFF;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return ImmutableList.of(this.toggleButton, this.helpButton);
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            for(GuiEventListener listener : children()) {
                if(listener.mouseClicked(mouseX, mouseY, button))
                    return true;
            }
            return false;
        }

        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            for(GuiEventListener listener : children()) {
                if(listener.mouseReleased(mouseX, mouseY, button))
                    return true;
            }
            return false;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return Collections.emptyList();
        }
    }

    public abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
        public Entry() {
        }
    }
}
