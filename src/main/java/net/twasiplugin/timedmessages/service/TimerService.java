package net.twasiplugin.timedmessages.service;

import com.google.gson.Gson;
import net.twasi.core.database.models.User;
import net.twasi.core.interfaces.api.TwasiInterface;
import net.twasi.core.logger.TwasiLogger;
import net.twasi.core.models.Streamer;
import net.twasi.core.plugin.api.TwasiCustomCommand;
import net.twasi.core.plugin.api.TwasiUserPlugin;
import net.twasi.core.services.IService;
import net.twasi.core.services.ServiceRegistry;
import net.twasi.core.services.providers.DataService;
import net.twasiplugin.commands.CommandRepository;
import net.twasiplugin.commands.CustomCommand;
import net.twasiplugin.timedmessages.Plugin;
import net.twasiplugin.timedmessages.database.TimerEntity;
import net.twasiplugin.timedmessages.database.TimerRepository;
import net.twasiplugin.timedmessages.service.exceptions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class TimerService implements IService {
    private HashMap<String, List<Timer>> registeredTimers;
    private TimerRepository repo;
    private CommandRepository commandRepo;

    public TimerService() {
        this.registeredTimers = new HashMap<>();
        repo = ServiceRegistry.get(DataService.class).get(TimerRepository.class);
        commandRepo = ServiceRegistry.get(DataService.class).get(CommandRepository.class);
    }

    public void startTimers(TwasiUserPlugin twasiUserPlugin) {
        TwasiLogger.log.debug("Starting timers for user " + twasiUserPlugin.getTwasiInterface().getStreamer().getUser().getTwitchAccount().getDisplayName());
        stopTimers(twasiUserPlugin); // Stop timers if already running

        List<Timer> list = new ArrayList<>(); // Create empty list
        List<String> nameList = new ArrayList<>(); // List to store names of running timer command names to prevent double registration of a timer
        registeredTimers.put(twasiUserPlugin.getTwasiInterface().getStreamer().getUser().getId().toString(), list); // put empty list into registeredTrackers
        User user = twasiUserPlugin.getTwasiInterface().getStreamer().getUser();

        for (TwasiCustomCommand command : twasiUserPlugin.getTwasiInterface().getCustomCommands()) { // Loop through all commands of all plugins
            TimerEntity entity = repo.getTimerForUserAndCommand(user, command.getFormattedCommandName()); // Check if there is a timer registered
            if (entity != null) {// If there is a timer registered...
                list.add(new Timer(twasiUserPlugin.getTwasiInterface(), command.getFormattedCommandName(), entity.getInterval(), entity.isEnabled())); // ... let the timer-party begin! :o
                nameList.add(command.getFormattedCommandName().toLowerCase());
            }
        }

        for (CustomCommand cmd : commandRepo.getAllCommands(user)) {
            TimerEntity entity = repo.getTimerForUserAndCommand(user, cmd.getName());
            if (entity != null && !nameList.contains(entity.getCommand().toLowerCase())) {
                list.add(new Timer(twasiUserPlugin.getTwasiInterface(), cmd.getName(), entity.getInterval(), entity.isEnabled())); // ... let the timer-party begin! :o
                nameList.add(cmd.getName().toLowerCase());
            }
        }

        TwasiLogger.log.debug("Started timers: " + new Gson().toJson(nameList));
    }

    // Function to stop (and remove from cache, not from db) all timers for user if there are any timers running
    public void stopTimers(TwasiUserPlugin twasiUserPlugin) {
        TwasiLogger.log.debug("Stopping timers for user " + twasiUserPlugin.getTwasiInterface().getStreamer().getUser().getTwitchAccount().getDisplayName());
        List<Timer> timers = registeredTimers.get(twasiUserPlugin.getTwasiInterface().getStreamer().getUser().getId().toString()); // Load timers

        if (timers != null) {
            for (Timer timer : timers) timer.setEnable(false); // Disable all timers if there are any
            registeredTimers.remove(twasiUserPlugin.getTwasiInterface().getStreamer().getUser().getId().toString()); // Remove timers if registered
        }
    }

    public Timer getTimerForUserAndCommand(User user, String command) {
        List<Timer> timers = registeredTimers.get(user.getId().toString()); // Load timers
        if (timers == null) return null; // Return null if there are no timers

        for (Timer t : timers)
            if (t.getCommand().equalsIgnoreCase(command)) return t; // Return the corresponding timer if it exists

        if (!command.startsWith(Plugin.botPrefix)) {
            return getTimerForUserAndCommand(user, Plugin.botPrefix + command);
        }

        return null; // Oh no, it does not exist :( return null instead
    }

    public TimerEntity getTimerEntityForUserAndCommand(TwasiUserPlugin user, String command) {
        for (TimerEntity entity : getTimersForUser(user))
            if (entity.getCommand().equalsIgnoreCase(command)) return entity;
        if (!command.startsWith(Plugin.botPrefix)) {
            return getTimerEntityForUserAndCommand(user, Plugin.botPrefix + command);
        }
        return null;
    }

    // Function to Query all active timers for user
    public List<TimerEntity> getTimersForUser(TwasiUserPlugin twasiUserPlugin) {
        List<TimerEntity> timerEntitiesForUser = repo.getTimerEntitiesForUser(twasiUserPlugin.getTwasiInterface().getStreamer().getUser()); // Get all timers from database

        if (timerEntitiesForUser == null || timerEntitiesForUser.size() == 0)
            return new ArrayList<>(); // Return empty list if there are no timers yet

        timerEntitiesForUser = new ArrayList<>(timerEntitiesForUser); // Make list editable by creating ArrayList
        HashMap<String, TimerEntity> timersAndNames = new HashMap<>(); // A map to store timers and there command as key in
        for (TimerEntity t : timerEntitiesForUser)
            timersAndNames.put(t.getCommand().toLowerCase(), t); // Add all timers from database

        List<TimerEntity> timers = new ArrayList<>(); // The list we will return at the end
        for (TwasiCustomCommand cmd : twasiUserPlugin.getTwasiInterface().getCustomCommands()) { // Loop through all available commands
            if (timersAndNames.containsKey(cmd.getFormattedCommandName().toLowerCase())) { // Check if there is a timer available for that command
                timers.add(timersAndNames.get(cmd.getFormattedCommandName().toLowerCase())); // And add the timer to the return list
            }
        }

        for (CustomCommand cmd : commandRepo.getAllCommands(twasiUserPlugin.getTwasiInterface().getStreamer().getUser())) {
            if (timersAndNames.containsKey(cmd.getName().toLowerCase())) {
                TimerEntity entity = timersAndNames.get(cmd.getName().toLowerCase());
                if (!timers.contains(entity)) timers.add(entity);
            }
        }

        timerEntitiesForUser.removeAll(timers); // Remove all used timers from the old list
        for (TimerEntity entity : timerEntitiesForUser) entity.setEnabled(false); // and disable all unused timers
        repo.commitAll(); // Write to database

        return timers; // And finally return our timers
    }

    public List<Timer> getRunningTimersForUser(User user) {
        return this.registeredTimers.get(user.getId().toString());
    }

    // Function to look up if there are currently timers running
    public boolean hasTimersEnabled(User user) {
        return registeredTimers.get(user.getId().toString()) != null; // Timers are enabled if there is a list (no matter whether empty or not)
    }

    // A function to add (and start if online) a timer
    public TimerEntity registerTimer(TwasiUserPlugin twasiUserPlugin, String command, int interval) throws TimerException {
        TwasiInterface twasiInterface = twasiUserPlugin.getTwasiInterface();
        Streamer streamer = twasiInterface.getStreamer();
        User user = streamer.getUser();
        TwasiLogger.log.debug("Trying to register new timer for user " + user.getTwitchAccount().getDisplayName() + " for command " + command);

        if (interval < 1) throw new TooLowIntervalException();

        TimerEntity timer = repo.getTimerForUserAndCommand(user, command);
        if (timer != null && getTimersForUser(twasiUserPlugin).stream().map(t -> t.getId().toString()).collect(Collectors.toList()).contains(timer.getId().toString()))
            throw new CommandAlreadyHasTimerException();

        boolean exists = false;
        for (TwasiCustomCommand cmd : twasiInterface.getCustomCommands())
            if ((cmd.getFormattedCommandName()).equalsIgnoreCase(command)) {
                if (!cmd.allowsTimer()) throw new CommandDoesNotAllowTimersException();
                exists = true;
                break;
            }
        if (!exists) {
            List<CustomCommand> allCommands = commandRepo.getAllCommands(user);
            for (CustomCommand cmd : allCommands)
                if (cmd.getName().equalsIgnoreCase(command)) exists = true;
        }
        if (!exists && !command.startsWith(Plugin.botPrefix)) {
            return registerTimer(twasiUserPlugin, Plugin.botPrefix + command, interval);
        }
        if (!exists) throw new CommandDoesNotExistException();

        if (timer != null) repo.removeTimerEntity(timer);
        timer = new TimerEntity(user, command, interval, true);
        repo.add(timer);
        TwasiLogger.log.debug("Timer was registered and committed to database");

        if (hasTimersEnabled(user)) {
            registeredTimers.get(user.getId().toString()).add(new Timer(twasiInterface, command, interval));
            TwasiLogger.log.debug("Timer was started");
        }

        return timer;
    }

    // Function to remove (and stop if running) a timer
    public TimerEntity removeTimer(TwasiUserPlugin twasiUserPlugin, String command) throws TimerException {
        TimerEntity entity = getTimerEntityForUserAndCommand(twasiUserPlugin, command); // Get corresponding timer
        if (entity == null) throw new CommandHasNoTimerException(); // Check if exists
        repo.removeTimerEntity(entity); // If it exists remove it from database

        User user = twasiUserPlugin.getTwasiInterface().getStreamer().getUser();
        if (hasTimersEnabled(user)) { // Check if there are timers running
            List<Timer> timers = registeredTimers.get(user.getId().toString()); // Get all running timers
            for (Timer timer : timers) // And loop through them
                if (timer.getCommand().equalsIgnoreCase(command)) { // If the name fits
                    timer.setEnable(false); // Stop it
                    timers.remove(timer); // And remove it
                    break; // No need to go on
                }
        }
        return entity;
    }

    // Function to activate/deactivate a specific timer
    public void enableTimer(TwasiUserPlugin twasiUserPlugin, String command, boolean enable) throws TimerException {
        TimerEntity entity = getTimerEntityForUserAndCommand(twasiUserPlugin, command); // Get corresponding timer
        if (entity == null) throw new CommandHasNoTimerException(); // Check if exists

        entity.setEnabled(enable); // Yay it exists. Lets set the new state
        repo.commit(entity); // And save it to database c:

        User user = twasiUserPlugin.getTwasiInterface().getStreamer().getUser();
        if (hasTimersEnabled(user)) { // Now let's take a look if there are currently timers running
            List<Timer> timers = registeredTimers.get(user.getId().toString()); // Get all running timers
            for (Timer timer : timers) // And loop through all of them
                if (timer.getCommand().equalsIgnoreCase(command)) { // See if it fits the command name
                    timer.setEnable(enable); // Set the enable state that was requested
                    break; // No need to go on, we already got what we wanted
                }
        }
    }

}
