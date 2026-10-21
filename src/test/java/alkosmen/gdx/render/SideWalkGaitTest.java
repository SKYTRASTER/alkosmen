package alkosmen.gdx.render;

/** Run with assertions; no graphics context is required. */
public final class SideWalkGaitTest {
    public static void main(String[] args) {
        int width = 384;
        int height = 752;
        int hipY = Math.round(height * 0.71f);
        for (int frame = 0; frame < SideWalkGait.FRAME_COUNT; frame++) {
            assert SideWalkGait.offsetX(frame, 175, hipY - 30, width, height, false) == 0;
            assert SideWalkGait.offsetX(frame, 175, hipY, width, height, false) == 0;
            int right = SideWalkGait.offsetX(frame, 175, 720, width, height, false);
            int left = SideWalkGait.offsetX(frame, 175, 720, width, height, true);
            assert right == -left : "Side-step swing must mirror by direction";
            assert Math.abs(right) <= 16 : "The stride must stay restrained";
            assert right == -SideWalkGait.offsetX(frame, 260, 720, width, height, false)
                : "The legs should swing in opposite directions";
        }
        assert SideWalkGait.offsetX(1, 175, 720, width, height, false) != 0;
        assert SideWalkGait.offsetX(1, 175, 720, width, height, false)
            == -SideWalkGait.offsetX(4, 175, 720, width, height, false);
        assert SideWalkGait.offsetX(0, 175, 720, width, height, false) == 0;
        assert SideWalkGait.offsetX(3, 175, 720, width, height, false) == 0;
        System.out.println("SideWalkGaitTest: OK");
    }
}
