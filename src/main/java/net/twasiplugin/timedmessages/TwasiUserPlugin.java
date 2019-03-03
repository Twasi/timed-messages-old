package net.twasiplugin.timedmessages;

import net.twasi.core.events.TwasiEventHandler;
import net.twasi.core.plugin.api.TwasiCustomCommand;
import net.twasi.core.plugin.api.events.TwasiDisableEvent;
import net.twasi.core.plugin.api.events.TwasiEnableEvent;
import net.twasi.core.plugin.api.events.TwasiInstallEvent;
import net.twasi.core.services.ServiceRegistry;
import net.twasiplugin.dependency.streamtracker.StreamTrackerService;
import net.twasiplugin.dependency.streamtracker.events.StreamStopEvent;
import net.twasiplugin.dependency.streamtracker.events.StreamTrackEvent;
import net.twasiplugin.timedmessages.commands.AddTimerCommand;
import net.twasiplugin.timedmessages.commands.DelTimerCommand;
import net.twasiplugin.timedmessages.commands.TimerCommand;

import java.util.ArrayList;
import java.util.List;

public class TwasiUserPlugin extends net.twasi.core.plugin.api.TwasiUserPlugin {

    private boolean active = true;
    private List<TwasiCustomCommand> commands = new ArrayList<>();

    @Override
    public void onInstall(TwasiInstallEvent e) {
        e.getAdminGroup().addKey("twasi.timer.*");
        e.getModeratorsGroup().addKey("twasi.timer.list");
        e.getModeratorsGroup().addKey("twasi.timer.enable");
        e.getModeratorsGroup().addKey("twasi.timer.disable");
    }

    @Override
    public void onUninstall(TwasiInstallEvent e) {
        e.getAdminGroup().removeKey("twasi.timer.*");
        e.getModeratorsGroup().removeKey("twasi.timer.list");
        e.getModeratorsGroup().removeKey("twasi.timer.enable");
        e.getModeratorsGroup().removeKey("twasi.timer.disable");
        active = false;
        Plugin.service.stopTimers(TwasiUserPlugin.this);
    }

    @Override
    public void onEnable(TwasiEnableEvent e) {
        commands.add(new TimerCommand(this));
        commands.add(new AddTimerCommand(this));
        commands.add(new DelTimerCommand(this));
        StreamTrackerService sts = ServiceRegistry.get(StreamTrackerService.class);
        sts.registerStreamTrackEvent(this.getTwasiInterface().getStreamer().getUser(), new StreamTrackerService.TwasiStreamTrackEventHandler() {
            @Override
            public void on(StreamTrackEvent streamTrackEvent) {
                if (active)
                    if (!Plugin.service.hasTimersEnabled(getTwasiInterface().getStreamer().getUser()))
                        Plugin.service.startTimers(TwasiUserPlugin.this);
            }
        });
        sts.registerStreamStopEvent(this.getTwasiInterface().getStreamer().getUser(), new TwasiEventHandler<StreamStopEvent>() {
            @Override
            public void on(StreamStopEvent streamStopEvent) {
                if (active)
                    Plugin.service.stopTimers(TwasiUserPlugin.this);
            }
        });
    }

    @Override
    public void onDisable(TwasiDisableEvent e) {
        active = false;
        Plugin.service.stopTimers(TwasiUserPlugin.this);
    }

    @Override
    public List<TwasiCustomCommand> getCommands() {
        return commands;
    }
}
