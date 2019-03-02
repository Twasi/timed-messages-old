package net.twasiplugin.timedmessages;

import net.twasi.core.events.TwasiEventHandler;
import net.twasi.core.plugin.api.TwasiCustomCommand;
import net.twasi.core.plugin.api.TwasiUserPlugin;
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
import java.util.concurrent.TimeUnit;

public class UserPlugin extends TwasiUserPlugin {

    private boolean online = false;

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
    }

    public UserPlugin() {
        Thread t1 = new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException ignored) {
            }
            commands.add(new TimerCommand(this));
            commands.add(new AddTimerCommand(this));
            commands.add(new DelTimerCommand(this));
            if (!online) Plugin.service.startTimers(UserPlugin.this); // TODO move back into event handler
            StreamTrackerService sts = ServiceRegistry.get(StreamTrackerService.class);
            sts.registerStreamTrackEvent(this.getTwasiInterface().getStreamer().getUser(), new StreamTrackerService.TwasiStreamTrackEventHandler() {
                @Override
                public void on(StreamTrackEvent streamTrackEvent) {
                    online = true;
                }
            });
            sts.registerStreamStopEvent(this.getTwasiInterface().getStreamer().getUser(), new TwasiEventHandler<StreamStopEvent>() {
                @Override
                public void on(StreamStopEvent streamStopEvent) {
                    if (online) Plugin.service.stopTimers(UserPlugin.this);
                    online = false;
                }
            });
        });
        t1.setDaemon(true);
        t1.start();
    }

    @Override
    public List<TwasiCustomCommand> getCommands() {
        return commands;
    }
}
