package alkosmen.gdx;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;

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
        assert !at(map, 720, 570) : "Entrance to courtyard blocked";
        assert !at(map, 800, 520) : "Yard blocked";
        assert at(map, 820, 390) : "House should block";
        assert at(map, 1000, 580) : "Fence should block";
        assert at(map, 300, 160) : "Trees should block";
        assert at(map, 1500, 150) : "Pond should block";
        assert at(map, 1280, 730) : "Rocks should block";
        assert at(map, 560, 830) : "Lamp should block";
        assert at(map, 5, 900) : "Map edge should block";
        for (Polygon polygon : map.colliders()) assert convex(polygon.getVertices()) : "Nonconvex obstacle";
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
