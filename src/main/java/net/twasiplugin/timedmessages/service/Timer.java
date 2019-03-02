package net.twasiplugin.timedmessages.service;

import net.twasi.core.database.models.TwitchAccount;
import net.twasi.core.database.models.User;
import net.twasi.core.database.models.permissions.PermissionGroups;
import net.twasi.core.interfaces.api.TwasiInterface;
import net.twasi.core.logger.TwasiLogger;
import net.twasi.core.models.Message.MessageType;
import net.twasi.core.models.Message.TwasiMessage;
import net.twasiplugin.timedmessages.Plugin;
import net.twasiplugin.timedmessages.service.exceptions.TimerNotRunningException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class Timer extends Thread {

    private final TwasiInterface streamerInf;
    private final String command;
    private final int interval;
    private int intervalRemaining; // How many time is remaining until the next timer-trigger
    private boolean enable;

    /**
     * @param streamerInf The TwasiInterface of the streamer in whose channel the timer should run
     * @param command     The command that should be executed
     * @param interval    The interval in seconds how often the timer should trigger
     * @param enable      Whether the timer should start activated or not
     */
    public Timer(TwasiInterface streamerInf, String command, int interval, boolean enable) {

        // Set fields
        this.streamerInf = streamerInf;
        this.command = command;
        this.interval = interval;
        this.intervalRemaining = interval;
        this.enable = enable;

        // Allow thread to be terminated
        this.setDaemon(true);

        if (enable) start();

        TwasiLogger.log.debug("Timer for user " + getTimerOwner().getTwitchAccount().getDisplayName() + " was started for command " + command);
    }

    public Timer(TwasiInterface streamerInf, String command, int interval) {
        this(streamerInf, command, interval, true);
    }

    @Override
    public void run() {
        while (enable) {
            try {
                TimeUnit.MINUTES.sleep(1); // Check every minute
                if (!enable) break; // Break if timer is disabled within this one second lol
            } catch (InterruptedException ignored) {
            }
            if (--intervalRemaining > 0) continue; // Wait for the next second if timer is not ready yet
            intervalRemaining = interval; // Reset timer if triggered
            try {
                TwasiLogger.log.debug("Dispatching timer message for user " + getTimerOwner().getTwitchAccount().getDisplayName() + " for command " + getCommand());
                TwitchAccount accToClone = getTimerOwner().getTwitchAccount();
                streamerInf.getDispatcher().dispatch( // Simulate command event
                        new TwasiMessage(
                                String.format("%s%s", Plugin.botPrefix, this.command),
                                MessageType.PRIVMSG,
                                new TwitchAccount(
                                        accToClone.getUserName(),
                                        accToClone.getDisplayName(),
                                        accToClone.getToken(),
                                        accToClone.getTwitchId(),
                                        new ArrayList<>(Collections.singletonList(PermissionGroups.BROADCASTER))
                                ),
                                streamerInf
                        )
                );
            } catch (Exception e) {
                TwasiLogger.log.warn("Exception while triggering timer for user " + getTimerOwner().getTwitchAccount().getDisplayName());
                e.printStackTrace();
            }
        }
    }

    public TwasiInterface getStreamerTwasiInterface() {
        return streamerInf;
    }

    public String getCommand() {
        return command;
    }

    public int getInterval() {
        return interval;
    }

    public boolean isEnable() {
        return enable;
    }

    public User getTimerOwner() {
        return streamerInf.getStreamer().getUser();
    }

    public void setEnable(boolean enable) {
        this.intervalRemaining = this.interval; // Reset interval (even if timer is still running)
        boolean wasAlreadyEnabled = this.enable; // Temporarily save old state
        this.enable = enable; // Enable new state (before starting loop to prevent the loop from instant shutdown)
        if (!wasAlreadyEnabled && enable) start(); // Start loop if it was not running
    }

    public int getNextTriggerInMinutes() throws TimerNotRunningException {
        if (!enable) throw new TimerNotRunningException();
        return intervalRemaining;
    }
}
