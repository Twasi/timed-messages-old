package net.twasiplugin.timedmessages.commands;

import net.twasi.core.plugin.api.TwasiCustomCommandEvent;
import net.twasi.core.plugin.api.TwasiUserPlugin;
import net.twasiplugin.timedmessages.Plugin;

public class DelTimerCommand extends BaseCommand {

    private TwasiUserPlugin plugin;

    public DelTimerCommand(TwasiUserPlugin twasiUserPlugin) {
        super(twasiUserPlugin);
        this.plugin = twasiUserPlugin;
    }

    @Override
    public String getCommandName() {
        return "deltimer";
    }

    @Override
    public void postProcess(TwasiCustomCommandEvent event) {
        try {
            Plugin.service.removeTimer(plugin.getTwasiInterface().getStreamer().getUser(), event.getArgs().get(0));
            event.reply("Erfolgreich entfernt #DEBUG");
        } catch (Exception e) {
            event.reply("Ein Fehler ist aufgetreten #DEBUG");
            e.printStackTrace();
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
