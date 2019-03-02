package net.twasiplugin.timedmessages.commands;

import net.twasi.core.plugin.api.TwasiCustomCommand;
import net.twasi.core.plugin.api.TwasiCustomCommandEvent;
import net.twasi.core.plugin.api.TwasiUserPlugin;

public abstract class BaseCommand extends TwasiCustomCommand {

    public BaseCommand(TwasiUserPlugin twasiUserPlugin) {
        super(twasiUserPlugin);
    }

    public abstract void postProcess(TwasiCustomCommandEvent event);

    public abstract String getHelpText();

    public String requirePermission() {
        return null;
    }

    @Override
    public final void process(TwasiCustomCommandEvent e) {
        if (requirePermission() != null && !e.getStreamer().getUser().hasPermission(e.getSender(), requirePermission()))
            return;
        if (!e.hasArgs()) e.reply(getHelpText());
        else postProcess(e);
    }
}
