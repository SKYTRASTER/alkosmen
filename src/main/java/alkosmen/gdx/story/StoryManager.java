package alkosmen.gdx.story;

/**
 * Keeps story advancement explicit. Invalid or repeated triggers are ignored,
 * so world objects cannot replay completed scenes by accident.
 */
public final class StoryManager {
    private StoryState state = StoryState.START;
    private Objective objective = new Objective("Осмотреть район");

    public StoryState state() {
        return state;
    }

    public Objective objective() {
        return objective;
    }

    public boolean trigger(StoryTrigger trigger) {
        return switch (trigger) {
            case KEY_COLLECTED -> transition(
                StoryState.START,
                StoryState.KEY_FOUND,
                "Осмотреться"
            );
            case POLICE_SCENE_FINISHED -> transition(
                StoryState.KEY_FOUND,
                StoryState.POLICE_SEEN,
                "Выслушать полицейских"
            );
            case STATION_OBJECTIVE_READY -> transition(
                StoryState.POLICE_SEEN,
                StoryState.STATION_OBJECTIVE,
                "Добраться до вокзала"
            );
        };
    }

    public void reset() {
        state = StoryState.START;
        objective = new Objective("Осмотреть район");
    }

    private boolean transition(StoryState expected, StoryState next, String nextObjective) {
        if (state != expected) {
            return false;
        }
        state = next;
        objective = new Objective(nextObjective);
        return true;
    }
}
