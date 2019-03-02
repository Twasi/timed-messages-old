package net.twasiplugin.timedmessages.commands;

import net.twasi.core.plugin.api.TwasiCustomCommandEvent;
import net.twasi.core.plugin.api.TwasiUserPlugin;
import net.twasiplugin.timedmessages.Plugin;
import net.twasiplugin.timedmessages.service.exceptions.CommandHasNoTimerException;

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
        String timer = event.getArgs().get(0);
        try {
            Plugin.service.removeTimer(plugin, timer);
            event.reply(getTranslation("twasi.timer.remove.success", timer));
        } catch (CommandHasNoTimerException e) {
            event.reply(getTranslation("twasi.timer.notfound", timer));
            e.printStackTrace();
        } catch (Exception e) {
            event.reply(getTranslation("twasi.timer.error"));
            e.printStackTrace();
        }
    }

    @Override
    public String getHelpText() {
        return getTranslation("twasi.timer.remove.help");
    }

    @Override
    public String requirePermission() {
        return "twasi.timer.delete";
    }
}
