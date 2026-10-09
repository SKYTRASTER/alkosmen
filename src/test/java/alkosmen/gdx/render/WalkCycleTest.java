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
        WalkCycle wrapped = new WalkCycle(6);
        wrapped.advance(122.4f * 2.25f, 122.4f);
        assert wrapped.frameIndex() == 1 : "Multi-cycle wrap failed";
        wrapped.reset();
        assert wrapped.frameIndex() == 0;
        try {
            wrapped.advance(1f, 0f);
            throw new AssertionError("Invalid cycle distance accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
        System.out.println("WalkCycle checks passed");
    }
}
