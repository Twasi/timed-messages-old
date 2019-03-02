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
            Plugin.service.registerTimer(plugin.getTwasiInterface(), command, Integer.parseInt(event.getArgs().get(1)));
            event.reply(getTranslation("twasi.timer.add.success", command));
        } catch (CommandDoesNotExistException e) {
            event.reply(getTranslation("twasi.timer.add.notfound", command));
            e.printStackTrace();
        } catch (CommandDoesNotAllowTimersException e) {
            event.reply(getTranslation("twasi.timer.add.notallowed", command));
            e.printStackTrace();
        } catch (CommandAlreadyHasTimerException e) {
            event.reply(getTranslation("twasi.timer.add.double", command));
            e.printStackTrace();
        } catch (TooLowIntervalException e) {
            event.reply(getTranslation("twasi.timer.add.lowinterval"));
            e.printStackTrace();
        } catch (NumberFormatException e) {
            event.reply(getHelpText());
            e.printStackTrace();
        } catch (Exception e) {
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
