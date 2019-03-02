package net.twasiplugin.timedmessages.service;

import net.twasi.core.database.models.User;
import net.twasi.core.interfaces.api.TwasiInterface;
import net.twasi.core.models.Streamer;
import net.twasi.core.plugin.api.TwasiCustomCommand;
import net.twasi.core.plugin.api.TwasiUserPlugin;
import net.twasi.core.services.IService;
import net.twasi.core.services.ServiceRegistry;
import net.twasi.core.services.providers.DataService;
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

    public void startTimers(TwasiUserPlugin twasiUserPlugin) {
        stopTimers(twasiUserPlugin); // Stop timers if already running
        List<Timer> list = new ArrayList<>(); // Create empty list
        registeredTimers.put(twasiUserPlugin.getTwasiInterface().getStreamer().getUser().getId().toString(), list); // put empty list into registeredTrackers
        for (TwasiCustomCommand command : twasiUserPlugin.getTwasiInterface().getCustomCommands()) { // Loop through all commands of all plugins
            TimerEntity entity = repo.getTimerForUserAndCommand(twasiUserPlugin.getTwasiInterface().getStreamer().getUser(), command.getCommandName()); // Check if there is a timer registered
            if (entity != null) {// If there is a timer registered...
                list.add(new Timer(twasiUserPlugin.getTwasiInterface(), command.getCommandName(), entity.getInterval(), entity.isEnabled())); // ... let the timer-party begin! :o
            }
        }
    }

    // Function to stop (and remove from cache, not from db) all timers for user if there are any timers running
    public void stopTimers(TwasiUserPlugin twasiUserPlugin) {
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
        return null; // Oh no, it does not exist :( return null instead
    }

    public TimerEntity getTimerEntityForUserAndCommand(TwasiUserPlugin user, String command) {
        for (TimerEntity entity : getTimersForUser(user))
            if (entity.getCommand().equalsIgnoreCase(command)) return entity;
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
            if (timersAndNames.containsKey(cmd.getCommandName().toLowerCase())) { // Check if there is a timer available for that command
                timers.add(timersAndNames.get(cmd.getCommandName().toLowerCase())); // And add the timer to the return list
            }
        }
        timerEntitiesForUser.removeAll(timers); // Remove all used timers from the old list
        for (TimerEntity entity : timerEntitiesForUser) entity.setEnabled(false); // and disable all unused timers
        repo.commitAll(); // Write to database
        return timers; // And finally return our timers
    }

    // Function to look up if there are currently timers running
    public boolean hasTimersEnabled(User user) {
        return registeredTimers.get(user.getId().toString()) != null; // Timers are enabled if there is a list (no matter whether empty or not)
    }

    // A function to add (and start if online) a timer
    public void registerTimer(TwasiInterface twasiInterface, String command, int interval) throws TimerException {
        Streamer streamer = twasiInterface.getStreamer();
        User user = streamer.getUser();
        if (interval < 1) throw new TooLowIntervalException();
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

    // Function to remove (and stop if running) a timer
    public void removeTimer(TwasiUserPlugin twasiUserPlugin, String command) throws TimerException {
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
