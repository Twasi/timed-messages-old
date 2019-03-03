package net.twasiplugin.timedmessages.commands;

import net.twasi.core.database.models.User;
import net.twasi.core.plugin.api.TwasiCustomCommandEvent;
import net.twasiplugin.timedmessages.Plugin;
import net.twasiplugin.timedmessages.TwasiUserPlugin;
import net.twasiplugin.timedmessages.database.TimerEntity;

import java.util.Arrays;
import java.util.List;

public class TimerCommand extends BaseCommand {

    private TwasiUserPlugin twasiUserPlugin;

    public TimerCommand(TwasiUserPlugin twasiUserPlugin) {
        super(twasiUserPlugin);
        this.twasiUserPlugin = twasiUserPlugin;
    }

    public String getCommandName() {
        return "timer";
    }

    @Override
    public void postProcess(TwasiCustomCommandEvent e) {
        User user = e.getStreamer().getUser();
        switch (e.getArgs().get(0).toLowerCase()) {
            case "list":
            case "info":
                if (!user.hasPermission(e.getSender(), "twasi.timer.list")) return;
                String enabled = getTranslation("twasi.timer.list.success.active"), disabled = getTranslation("twasi.timer.list.success.inactive");
                if (e.getArgs().size() < 2) {
                    List<TimerEntity> timers = Plugin.service.getTimersForUser(twasiUserPlugin);
                    if (timers == null || timers.size() == 0) {
                        e.reply(getTranslation("twasi.timer.list.notimers"));
                        return;
                    }
                    StringBuilder timerString = new StringBuilder();
                    for (TimerEntity entity : timers) {
                        timerString.append(", ").append(getTranslation("twasi.timer.list.success.multi.format", entity.getCommand(), entity.getInterval(), entity.isEnabled() ? enabled : enabled));
                    }
                    e.reply(getTranslation("twasi.timer.list.success.multi", timerString.toString().substring(2)));
                } else {
                    String command = e.getArgs().get(1);
                    TimerEntity entity = Plugin.service.getTimerEntityForUserAndCommand(twasiUserPlugin, command);
                    if (entity == null) {
                        e.reply(getTranslation("twasi.timer.notfound", command));
                    } else {
                        e.reply(getTranslation("twasi.timer.list.success.single", entity.getCommand(), entity.getInterval(), entity.isEnabled() ? enabled : disabled));
                    }
                }
                return;
            case "enable":
                if (!user.hasPermission(e.getSender(), "twasi.timer.enable")) return;
                if (e.getArgs().size() < 2) {

                } else {

                }
                break;
            case "disable":
                if (!user.hasPermission(e.getSender(), "twasi.timer.disable")) return;
                if (e.getArgs().size() < 2) {

                } else {

                }
                break;
            default:
                for (String s : Arrays.asList("list", "enable", "disable")) { // Loop through permission required to use one of the subcommands
                    if (user.hasPermission(e.getSender(), "twasi.timer." + s)) { // If user has one of the permissions
                        e.reply(getHelpText()); // Send help text
                        return; // Return to not execute twice if a user has more than one of the required permissions
                    }
                }
                break;
        }
    }

    @Override
    public String getHelpText() {
        return getTranslation("twasi.timer.help");
    }
}
