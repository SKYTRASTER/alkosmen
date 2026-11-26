package alkosmen.gdx.render;

/** Run with assertions; no libGDX graphics context is required. */
public final class SideWalkGaitTest {
    public static void main(String[] args) {
        int width = 384, height = 752, hipY = Math.round(height * .68f);
        for (int frame = 0; frame < SideWalkGait.FRAME_COUNT; frame++) {
            assert SideWalkGait.offsetX(frame, 150, hipY - 5, width, height, false) == 0;
            assert SideWalkGait.offsetY(frame, 150, hipY - 5, width, height, false) == 0;
            assert SideWalkGait.backOffsetX(frame, 150, hipY - 5, width, height) == 0;
            assert SideWalkGait.backOffsetY(frame, 150, hipY - 5, width, height) == 0;
            int right = SideWalkGait.offsetX(frame, 150, 720, width, height, false);
            int left = SideWalkGait.offsetX(frame, 234, 720, width, height, true);
            assert right == -left : "Side-walk mirrored stride mismatch";
            assert Math.abs(right) <= 38 : "Side stride too large";
        }
        assert SideWalkGait.offsetX(2, 150, 720, width, height, false) != 0;
        assert SideWalkGait.offsetX(2, 150, 720, width, height, false)
            == -SideWalkGait.offsetX(5, 150, 720, width, height, false);
        assert SideWalkGait.offsetY(2, 234, 720, width, height, false) < 0;
        assert SideWalkGait.backOffsetY(2, 234, 720, width, height) < 0;
        assert SideWalkGait.offsetX(0, 150, 720, width, height, false) == 0;
        assert SideWalkGait.backOffsetY(3, 234, 720, width, height) == 0;
        System.out.println("SideWalkGaitTest: OK");
    }
}
