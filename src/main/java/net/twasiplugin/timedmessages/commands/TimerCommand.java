package net.twasiplugin.timedmessages.commands;

import net.twasi.core.database.models.User;
import net.twasi.core.plugin.api.TwasiCustomCommandEvent;
import net.twasiplugin.timedmessages.UserPlugin;

public class TimerCommand extends BaseCommand {

    public TimerCommand(UserPlugin plugin) {
        super(plugin);
    }

    public String getCommandName() {
        return "timer";
    }

    @Override
    public void postProcess(TwasiCustomCommandEvent e) {
        User user = e.getStreamer().getUser();
        switch (e.getArgs().get(0).toLowerCase()) {
            case "list":
                if (!user.hasPermission(e.getSender(), "twasi.timer.list")) return;

                break;
            case "enable":
                if (!user.hasPermission(e.getSender(), "twasi.timer.enable")) return;

                break;
            case "disable":
                if (!user.hasPermission(e.getSender(), "twasi.timer.disable")) return;

                break;
        }
    }

    @Override
    public String getHelpText() {
        return null;
    }
}
