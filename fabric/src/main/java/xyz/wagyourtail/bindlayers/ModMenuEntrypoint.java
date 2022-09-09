package xyz.wagyourtail.bindlayers;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import xyz.wagyourtail.bindlayers.screen.LayerManagementScreen;

public class ModMenuEntrypoint implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return LayerManagementScreen::new;
    }

}
