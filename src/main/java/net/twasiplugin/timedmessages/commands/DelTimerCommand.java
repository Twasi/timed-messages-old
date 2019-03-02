package net.twasiplugin.timedmessages.commands;

import net.twasi.core.interfaces.api.TwasiInterface;
import net.twasi.core.plugin.api.TwasiCustomCommandEvent;
import net.twasi.core.plugin.api.TwasiUserPlugin;
import net.twasi.core.services.ServiceRegistry;
import net.twasiplugin.timedmessages.Plugin;
import net.twasiplugin.timedmessages.service.TimerService;

public class DelTimerCommand extends BaseCommand {

    private TimerService service = ServiceRegistry.getService(TimerService.class);
    private TwasiInterface twasiInterface;

    public DelTimerCommand(TwasiUserPlugin twasiUserPlugin) {
        super(twasiUserPlugin);
        this.twasiInterface = twasiUserPlugin.getTwasiInterface();
    }

    @Override
    public String getCommandName() {
        return "deltimer";
    }

    @Override
    public void postProcess(TwasiCustomCommandEvent event) {
        try {
            Plugin.service.removeTimer(twasiInterface.getStreamer().getUser(), event.getArgs().get(0));
            event.reply("Erfolgreich entfernt #DEBUG");
        } catch (Exception e) {
            event.reply("Ein Fehler ist aufgetreten #DEBUG");
        }
    }

    @Override
    public String getHelpText() {
        return null;
    }

    @Override
    public String requirePermission() {
        return "twasi.timer.delete";
    }
}
