package net.twasiplugin.timedmessages.commands;

import net.twasi.core.plugin.api.TwasiCustomCommandEvent;
import net.twasi.core.plugin.api.TwasiUserPlugin;
import net.twasiplugin.timedmessages.Plugin;
import net.twasiplugin.timedmessages.service.exceptions.CommandAlreadyHasTimerException;
import net.twasiplugin.timedmessages.service.exceptions.CommandDoesNotAllowTimersException;
import net.twasiplugin.timedmessages.service.exceptions.CommandDoesNotExistException;
import net.twasiplugin.timedmessages.service.exceptions.TooLowIntervalException;

public class AddTimerCommand extends BaseCommand {

    private TwasiUserPlugin plugin;

    public AddTimerCommand(TwasiUserPlugin twasiUserPlugin) {
        super(twasiUserPlugin);
        this.plugin = twasiUserPlugin;
    }

    @Override
    public String getCommandName() {
        return "addtimer";
    }

    @Override
    public void postProcess(TwasiCustomCommandEvent event) {
        if (event.getArgs().size() < 2) {
            event.reply(getHelpText());
            return;
        }
        String command = event.getArgs().get(0);
        try {
            int interval = Integer.parseInt(event.getArgs().get(1));
            Plugin.service.registerTimer(plugin.getTwasiInterface(), command, interval);
            event.reply(getTranslation("twasi.timer.add.success", command, interval, getTranslation("twasi.timer.add.success.minute" + (interval > 1 ? "s" : ""))));
        } catch (CommandDoesNotExistException e) {
            event.reply(getTranslation("twasi.timer.add.notfound", command));
        } catch (CommandDoesNotAllowTimersException e) {
            event.reply(getTranslation("twasi.timer.add.notallowed", command));
        } catch (CommandAlreadyHasTimerException e) {
            event.reply(getTranslation("twasi.timer.add.double", command));
        } catch (TooLowIntervalException e) {
            event.reply(getTranslation("twasi.timer.add.lowinterval"));
        } catch (NumberFormatException e) {
            event.reply(getHelpText());
        } catch (Exception e) {
            event.reply(getTranslation("twasi.timer.error"));
            e.printStackTrace();
        }
    }

    @Override
    public String getHelpText() {
        return getTranslation("twasi.timer.add.help");
    }

    @Override
    public String requirePermission() {
        return "twasi.timer.add";
    }
}
