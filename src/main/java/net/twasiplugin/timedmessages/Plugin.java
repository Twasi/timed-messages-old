package net.twasiplugin.timedmessages;

import net.twasi.core.plugin.TwasiPlugin;
import net.twasi.core.plugin.api.TwasiUserPlugin;
import net.twasi.core.services.ServiceRegistry;
import net.twasi.core.services.providers.config.ConfigService;
import net.twasiplugin.timedmessages.service.TimerService;

public class Plugin extends TwasiPlugin {

    public static String botPrefix;
    public static TimerService service;

    @Override
    public void onActivate() {
        ServiceRegistry.register(service = new TimerService());
        ConfigService configService = ServiceRegistry.getService(ConfigService.class);
        botPrefix = configService.getCatalog().bot.prefix;
    }

    public Class<? extends TwasiUserPlugin> getUserPluginClass() {
        return UserPlugin.class;
    }

}
