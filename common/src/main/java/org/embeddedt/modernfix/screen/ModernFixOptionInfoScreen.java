package org.embeddedt.modernfix.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

public class ModernFixOptionInfoScreen extends Screen {
    private final Screen lastScreen;
    private final Component description;

    public ModernFixOptionInfoScreen(Screen lastScreen, String optionName) {
        super(new TextComponent(optionName));

        this.lastScreen = lastScreen;
        this.description = new TranslatableComponent("modernfix.option." + optionName);
    }

    @Override
    protected void init() {
        super.init();
        this.addButton(new Button(this.width / 2 - 100, this.height - 29, 200, 20, "Done", (button) -> {
            this.onClose();
        }));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(lastScreen);
    }

    private void drawMultilineString(Font fr, Component str, int x, int y) {
        for(String s : fr.split(str.getString(), this.width - 50)) {
            fr.drawShadow(s, (float)x, (float)y, 16777215);
            y += fr.lineHeight;
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();
        drawCenteredString(this.font, this.title.getString(), this.width / 2, 8, 16777215);
        this.drawMultilineString(this.minecraft.font, description, 10, 50);
        super.render(mouseX, mouseY, partialTicks);
    }
}
