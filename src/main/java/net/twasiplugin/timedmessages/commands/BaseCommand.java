package net.twasiplugin.timedmessages.commands;

import net.twasi.core.plugin.api.TwasiUserPlugin;
import net.twasi.core.plugin.api.customcommands.TwasiCustomCommandEvent;
import net.twasi.core.plugin.api.customcommands.TwasiPluginCommand;

public abstract class BaseCommand extends TwasiPluginCommand {

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
