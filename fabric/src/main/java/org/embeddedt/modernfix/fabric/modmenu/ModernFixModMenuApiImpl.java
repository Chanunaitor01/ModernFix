package org.embeddedt.modernfix.fabric.modmenu;

import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;
import org.embeddedt.modernfix.screen.ModernFixConfigScreen;

@SuppressWarnings("unused")
public class ModernFixModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<ModernFixConfigScreen> getModConfigScreenFactory() {
        return ModernFixConfigScreen::new;
    }
}
