package alkosmen.gdx;

/** Standalone regression checks; run with assertions enabled. */
public final class SprintStateTest {
    public static void main(String[] args) {
        for (int fps : new int[] {30, 120}) {
            SprintState state = new SprintState();
            float delta = 1f / fps;
            assert state.speedMultiplier(delta, true) == SprintState.SPEED_MULTIPLIER;
            for (int frame = 0; frame < fps * 4; frame++) state.update(delta, true, true);
            assert state.exhausted() : "Must exhaust after four seconds at " + fps + " FPS";
            assert state.fraction() == 0f;
            assert state.speedMultiplier(delta, true) == 1f;
            // Holding Shift after exhaustion must allow recovery instead of rapid sprint toggles.
            for (int frame = 0; frame < fps; frame++) state.update(delta, true, true);
            assert state.exhausted() : "Recovery hysteresis missing";
            for (int frame = 0; frame < fps * 2; frame++) state.update(delta, false, false);
            assert !state.exhausted();
            state.update(10f, false, false);
            assert state.fraction() == 1f : "Recovery must clamp at full";
        }
        SprintState blocked = new SprintState();
        blocked.update(4f, true, false);
        assert blocked.fraction() == 1f : "Wall collision must not drain stamina";
        assert !blocked.sprinting();
        SprintState partial = new SprintState();
        partial.update(3.99f, true, true);
        float boost = partial.speedMultiplier(0.05f, true);
        assert boost > 1f && boost < SprintState.SPEED_MULTIPLIER : "Last frame must be blended";
        partial.update(0.05f, true, true);
        assert partial.exhausted();
        System.out.println("Sprint stamina checks passed");
    }
}
