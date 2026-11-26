package alkosmen.gdx;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.EarClippingTriangulator;
import java.util.ArrayDeque;

/** Tests authored geometry in image coordinates, without a graphics context. */
public final class ZuevkaSectorMapTest {
    public static void main(String[] args) {
        ZuevkaSectorMap map = new ZuevkaSectorMap(new FileHandle(
            "src/main/resources/" + ZuevkaSectorMap.BASE + "objects.json"));
        assert map.width == 1672 && map.height == 941;
        assert !map.blocks(AlkosmenPlayer.footprint(map.spawnX, map.spawnY)) : "Blocked spawn";
        for (int x = 650; x <= 1550; x += 10) {
            assert !at(map, x, 900) : "Road blocked at " + x;
        }
        assert !at(map, 750, 570) : "Entrance to courtyard blocked";
        assert !at(map, 755, 540) : "Yard entrance blocked";
        assert !at(map, 720, 495) : "Porch steps blocked";
        assert canReach(map, 660, 745, 720, 495) : "No route from spawn to porch";
        assert canReach(map, 660, 745, 1140, 440) : "No route from spawn to the garden gnome";
        assert at(map, 850, 490) : "Yard table should block";
        assert at(map, 570, 480) : "Porch woodpile should block";
        assert at(map, 650, 310) : "Porch roof should not be walkable";
        assert at(map, 850, 220) : "House roof should not be walkable";
        assert at(map, 246, 690) : "Sign post base should block";
        assert !at(map, 310, 660) : "Space beside the sign should be open";
        assert at(map, 820, 390) : "House should block";
        assert at(map, 1000, 580) : "Fence should block";
        assert at(map, 300, 160) : "Trees should block";
        assert at(map, 1500, 150) : "Pond should block";
        assert at(map, 1280, 730) : "Rocks should block";
        assert at(map, 560, 870) : "Lamp should block";
        assert at(map, 5, 900) : "Map edge should block";
        for (Polygon polygon : map.colliders()) assert convex(polygon.getVertices()) : "Nonconvex obstacle";
        for (ZuevkaSectorMap.Occluder occluder : map.occluders()) {
            assert occluder.inFrontOf(occluder.y() + 1f);
            assert !occluder.inFrontOf(occluder.y() - 1f);
            assert occluder.vertices().length >= 6;
            assert new EarClippingTriangulator().computeTriangles(occluder.vertices()).size
                == (occluder.vertices().length / 2 - 2) * 3 : "Invalid foreground polygon";
        }
        assert map.interactions().size() == 4;
        for (ZuevkaSectorMap.Interaction interaction : map.interactions()) {
            boolean reachable = false;
            for (float x = interaction.x() - interaction.radius(); x <= interaction.x() + interaction.radius(); x += 5f) {
                for (float y = interaction.y() - interaction.radius(); y <= interaction.y() + interaction.radius(); y += 5f) {
                    if (interaction.inRange(x, y) && !map.blocks(AlkosmenPlayer.footprint(x, y))) {
                        reachable = true;
                    }
                }
            }
            assert reachable : "No standing space in interaction range: " + interaction.id();
        }
        System.out.println("Zuevka sector geometry checks passed");
    }

    private static boolean at(ZuevkaSectorMap map, float imageX, float imageY) {
        Rectangle feet = AlkosmenPlayer.footprint(imageX, map.height - imageY);
        return map.blocks(feet);
    }

    private static boolean canReach(ZuevkaSectorMap map, int startX, int startY, int targetX, int targetY) {
        int step = 5;
        int width = (int) map.width / step + 1;
        int height = (int) map.height / step + 1;
        boolean[][] visited = new boolean[width][height];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        int start = startY / step * width + startX / step;
        queue.add(start);
        visited[startX / step][startY / step] = true;
        int[] dx = {1, -1, 0, 0}, dy = {0, 0, 1, -1};
        while (!queue.isEmpty()) {
            int cell = queue.removeFirst(), x = cell % width, y = cell / width;
            if (x == targetX / step && y == targetY / step) return true;
            for (int i = 0; i < dx.length; i++) {
                int nextX = x + dx[i], nextY = y + dy[i];
                if (nextX >= 0 && nextY >= 0 && nextX < width && nextY < height
                    && !visited[nextX][nextY] && !at(map, nextX * step, nextY * step)) {
                    visited[nextX][nextY] = true;
                    queue.add(nextY * width + nextX);
                }
            }
        }
        return false;
    }

    private static boolean convex(float[] v) {
        float sign = 0f;
        int n = v.length / 2;
        for (int i = 0; i < n; i++) {
            int a = i * 2, b = ((i + 1) % n) * 2, c = ((i + 2) % n) * 2;
            float cross = (v[b] - v[a]) * (v[c + 1] - v[b + 1])
                - (v[b + 1] - v[a + 1]) * (v[c] - v[b]);
            if (cross != 0f) {
                if (sign != 0f && Math.signum(cross) != sign) return false;
                sign = Math.signum(cross);
            }
        }
        return sign != 0f;
    }
}
