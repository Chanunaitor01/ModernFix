package org.embeddedt.modernfix.screen;

import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;

public class ModernFixConfigScreen extends Screen {
    private OptionList optionList;
    private Screen lastScreen;

    public boolean madeChanges = false;
    private Button doneButton, wikiButton;
    private double lastScrollAmount = 0;

    public ModernFixConfigScreen(Screen lastScreen) {
        super(new TranslatableComponent("modernfix.config"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        this.optionList = new OptionList(this, this.minecraft);
        this.optionList.setScrollAmount(lastScrollAmount);
        this.children.add(this.optionList);
        this.wikiButton = new Button(this.width / 2 - 155, this.height - 29, 150, 20, new TranslatableComponent("modernfix.config.wiki").getString(), (arg) -> {
            Util.getPlatform().openUri("https://github.com/embeddedt/ModernFix/wiki/Summary-of-Patches");
        });
        this.doneButton = new Button(this.width / 2 - 155 + 160, this.height - 29, 150, 20, "Done", (arg) -> {
            this.onClose();
        });
        this.addButton(this.wikiButton);
        this.addButton(this.doneButton);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(lastScreen);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();
        this.optionList.render(mouseX, mouseY, partialTicks);
        drawCenteredString(this.font, this.title.getString(), this.width / 2, 8, 16777215);
        this.doneButton.setMessage(madeChanges ? new TranslatableComponent("modernfix.config.done_restart").getString() : "Done");
        super.render(mouseX, mouseY, partialTicks);
    }

    public void setLastScrollAmount(double d) {
        this.lastScrollAmount = d;
    }
}
