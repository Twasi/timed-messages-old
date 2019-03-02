package net.twasiplugin.timedmessages.service;

import net.twasi.core.database.models.User;
import net.twasi.core.interfaces.api.TwasiInterface;
import net.twasi.core.logger.TwasiLogger;
import net.twasi.core.models.Streamer;
import net.twasi.core.plugin.api.TwasiCustomCommand;
import net.twasi.core.services.IService;
import net.twasi.core.services.ServiceRegistry;
import net.twasi.core.services.providers.DataService;
import net.twasiplugin.timedmessages.UserPlugin;
import net.twasiplugin.timedmessages.database.TimerEntity;
import net.twasiplugin.timedmessages.database.TimerRepository;
import net.twasiplugin.timedmessages.service.exceptions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TimerService implements IService {
    private HashMap<String, List<Timer>> registeredTimers;
    private TimerRepository repo;

    public TimerService() {
        this.registeredTimers = new HashMap<>();
        repo = ServiceRegistry.get(DataService.class).get(TimerRepository.class);
    }

    public void startTimers(UserPlugin userPlugin) {
        TwasiLogger.log.info(123);
        stopTimers(userPlugin); // Stop timers if already running
        List<Timer> list = new ArrayList<>(); // Create empty list
        registeredTimers.put(userPlugin.getTwasiInterface().getStreamer().getUser().getId().toString(), list); // put empty list into registeredTrackers
        for (TwasiCustomCommand command : userPlugin.getTwasiInterface().getCustomCommands()) { // Loop through all commands of all plugins
            TwasiLogger.log.info("Command: " + command.getCommandName());
            TimerEntity entity = repo.getTimerForUserAndCommand(userPlugin.getTwasiInterface().getStreamer().getUser(), command.getCommandName()); // Check if there is a timer registered
            if (entity != null) {// If there is a timer registered...
                TwasiLogger.log.info("Has Timer!");
                list.add(new Timer(userPlugin.getTwasiInterface(), command.getCommandName(), entity.getInterval(), entity.isEnabled())); // ... let the timer-party begin! :o
            }
        }
    }

    public void stopTimers(UserPlugin userPlugin) {
        List<Timer> timers = registeredTimers.get(userPlugin.getTwasiInterface().getStreamer().getUser().getId().toString()); // Load timers
        if (timers != null) {
            for (Timer timer : timers) timer.setEnable(false); // Disable all timers if there are any
            registeredTimers.remove(userPlugin.getTwasiInterface().getStreamer().getUser().getId().toString()); // Remove timers if registered
        }
    }

    public Timer getTimerForUserAndCommand(User user, String command) {
        List<Timer> timers = registeredTimers.get(user.getId().toString()); // Load timers
        if (timers == null) return null; // Return null if there are no timers
        for (Timer t : timers)
            if (t.getCommand().equalsIgnoreCase(command)) return t; // Return the corresponding timer if it exists
        return null; // Oh no, it does not exist :( return null instead
    }

    public boolean hasTimersEnabled(User user) {
        return registeredTimers.get(user.getId().toString()) != null; // Timers are enabled if there is a list (no matter whether empty or not)
    }

    public void registerTimer(TwasiInterface twasiInterface, String command, int interval) throws TimerException {
        Streamer streamer = twasiInterface.getStreamer();
        User user = streamer.getUser();
        if (interval < 30) throw new TooLowIntervalException();
        TimerEntity timer = repo.getTimerForUserAndCommand(user, command);
        if (timer != null) throw new CommandAlreadyHasTimerException();
        boolean exists = false;
        for (TwasiCustomCommand cmd : twasiInterface.getCustomCommands())
            if (cmd.getCommandName().equalsIgnoreCase(command)) {
                if (!cmd.allowsTimer()) throw new CommandDoesNotAllowTimersException();
                exists = true;
                break;
            }
        if (!exists) throw new CommandDoesNotExistException();
        timer = new TimerEntity(user, command, interval, true);
        repo.add(timer);
        if (hasTimersEnabled(user)) {
            registeredTimers.get(user.getId().toString()).add(new Timer(twasiInterface, command, interval));
        }
    }

    public void removeTimer(User user, String command) throws TimerException {
        TimerEntity entity = repo.getTimerForUserAndCommand(user, command);
        if (entity == null) throw new CommandHasNoTimerException();
        repo.removeTimerEntity(entity);
        if (hasTimersEnabled(user)) {
            List<Timer> timers = registeredTimers.get(user.getId().toString());
            for (Timer timer : timers)
                if (timer.getCommand().equalsIgnoreCase(command)) {
                    timer.setEnable(false);
                    timers.remove(timer);
                    break;
                }
        }
    }

}
