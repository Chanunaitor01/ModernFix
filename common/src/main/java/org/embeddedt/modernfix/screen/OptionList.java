package org.embeddedt.modernfix.screen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.*;
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

    private static final Component OPTION_ON = new TranslatableComponent("modernfix.option.on").withStyle(style -> style.setColor(ChatFormatting.GREEN));
    private static final Component OPTION_OFF = new TranslatableComponent("modernfix.option.off").withStyle(style -> style.setColor(ChatFormatting.RED));

    private static final Set<String> OPTIONS_MISSING_HELP = new HashSet<>();

    private ModernFixConfigScreen mainScreen;

    private static BaseComponent getOptionComponent(Option option) {
        String friendlyKey = "modernfix.option.name." + option.getName();
        TextComponent baseComponent = new TextComponent(option.getSelfName());
        if(I18n.exists(friendlyKey))
            return new TranslatableComponent(friendlyKey);
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
            int w = this.minecraft.font.width(getOptionComponent(option).getString()) + DEPTH_OFFSET * option.getDepth();
            this.maxNameWidth = Math.max(w, this.maxNameWidth);
            this.addEntry(new OptionEntry(option.getName(), option));
            ModernFixMixinPlugin.instance.config.getOptionMap().values().stream()
                    .filter(subOption -> subOption.getParent() == option)
                    .sorted(Comparator.comparing(Option::getName))
                    .forEach(this::addOption);
        }
    }

    public OptionList(ModernFixConfigScreen arg, Minecraft arg2) {
        super(arg2,arg.width + 45, arg.height, 43, arg.height - 32, 20);

        this.mainScreen = arg;

        Multimap<String, Option> optionsByCategory = ModernFixMixinPlugin.instance.config.getOptionCategoryMap();
        List<String> theCategories = OptionCategories.getCategoriesInOrder();
        for(String category : theCategories) {
            String categoryTranslationKey = "modernfix.option.category." + category;
            this.addEntry(new CategoryEntry(new TranslatableComponent(categoryTranslationKey)
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
            this.width = OptionList.this.minecraft.font.width(this.name.getString());
        }

        public void render(int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
            Font var10000 = OptionList.this.minecraft.font;
            float x = (float)(OptionList.this.minecraft.screen.width / 2 - this.width / 2);
            int y = top + height - 10;
            var10000.draw(this.name.getString(), x, y, 16777215);
        }

        public boolean changeFocus(boolean focus) {
            return false;
        }

        public List<? extends GuiEventListener> children() {
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
            this.toggleButton = new Button(0, 0, 55, 20, new TextComponent("").toString(), (arg) -> {
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
            });
            updateStatus();
            this.helpButton = new Button(75, 0, 20, 20, new TextComponent("?").getString(), (arg) -> {
                mainScreen.setLastScrollAmount(getScrollAmount());
                Minecraft.getInstance().setScreen(new ModernFixOptionInfoScreen(mainScreen, optionName));
            });
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
        public void render(int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
            BaseComponent nameComponent = getOptionComponent(option);
            if(this.option.isUserDefined())
                nameComponent = (BaseComponent) nameComponent.withStyle(style -> style.setItalic(true)).append(new TranslatableComponent("modernfix.config.not_default"));
            float textX = (float)(left + DEPTH_OFFSET * option.getDepth() + 160 - OptionList.this.maxNameWidth);
            float textY = (float)(top + height / 2 - 4);
            OptionList.this.minecraft.font.draw(nameComponent.getString(), textX, textY, 16777215);
            this.toggleButton.x = left + 175;
            this.toggleButton.y = top;
            this.toggleButton.setMessage(getOptionMessage(this.option).getString());
            this.toggleButton.render(mouseX, mouseY, partialTicks);
            this.helpButton.x = left + 175 + 55;
            this.helpButton.y = top;
            this.helpButton.render(mouseX, mouseY, partialTicks);
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
    }

    public abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
        public Entry() {
        }
    }
}
