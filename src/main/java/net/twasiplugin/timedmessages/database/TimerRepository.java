package net.twasiplugin.timedmessages.database;

import net.twasi.core.database.lib.Repository;
import net.twasi.core.database.models.User;

import java.util.List;

public class TimerRepository extends Repository<TimerEntity> {

    public TimerEntity getTimerForUserAndCommand(User user, String command) {
        try {
            return store.createQuery(TimerEntity.class).field("user").equal(user).field("command").equal(command.toLowerCase()).get();
        } catch (Exception e) {
            return null;
        }
    }

    public List<TimerEntity> getTimerEntitiesForUser(User user) {
        try {
            return store.createQuery(TimerEntity.class).field("user").equal(user).asList();
        } catch (Exception e) {
            return null;
        }
    }

    public void removeTimerEntity(TimerEntity entity) {
        store.delete(entity);
    }

}
