package alkosmen.gdx.render;

/** Standalone regression checks: run with java -ea, no graphics context required. */
public final class WalkCycleTest {
    public static void main(String[] args) {
        WalkCycle at30Fps = new WalkCycle(6);
        WalkCycle at120Fps = new WalkCycle(6);
        for (int i = 0; i < 15; i++) at30Fps.advance(135f / 30f, 122.4f);
        for (int i = 0; i < 60; i++) at120Fps.advance(135f / 120f, 122.4f);
        assert at30Fps.frameIndex() == at120Fps.frameIndex() : "Gait depends on FPS";
        assert at30Fps.frameIndex() == 3 : "Incorrect distance-driven frame";
        int frameBeforeStop = at30Fps.frameIndex();
        at30Fps.advance(0f, 122.4f);
        assert at30Fps.frameIndex() == frameBeforeStop : "No movement must preserve the gait phase";
        WalkCycle tapped = new WalkCycle(6);
        tapped.step();
        assert tapped.frameIndex() == 1 : "A quick tap must change the frame";
        tapped.advance(0f, 122.4f);
        assert tapped.frameIndex() == 1 : "Releasing the key must freeze the frame";
        for (int i = 0; i < 5; i++) tapped.step();
        assert tapped.frameIndex() == 0 : "Taps must wrap around the cycle";
        WalkCycle wrapped = new WalkCycle(6);
        wrapped.advance(122.4f * 2.25f, 122.4f);
        assert wrapped.frameIndex() == 1 : "Multi-cycle wrap failed";
        wrapped.reset();
        assert wrapped.frameIndex() == 0;

        WalkCycle playback = new WalkCycle(6);
        playback.update(0.21f, 0f, 122.4f);
        assert !playback.moving() && playback.idleFrameIndex() == 1;
        playback.update(0.01f, 1f, 122.4f);
        assert playback.moving() && playback.frameIndex() == 1;
        playback.update(0.15f, 122.4f / 2f, 122.4f);
        assert playback.moving() && playback.frameIndex() == 4 : "The opposite leg did not advance";
        playback.update(0.25f, 0f, 122.4f);
        assert !playback.moving() && playback.idleFrameIndex() == 0 : "Stop must select neutral idle";
        playback.update(0.21f, 0f, 122.4f);
        assert playback.idleFrameIndex() == 1 && playback.frameIndex() == 4;
        playback.reset();
        assert !playback.moving() && playback.idleFrameIndex() == 0 && playback.frameIndex() == 0;
        try {
            wrapped.advance(1f, 0f);
            throw new AssertionError("Invalid cycle distance accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
        System.out.println("WalkCycle checks passed");
    }
}
