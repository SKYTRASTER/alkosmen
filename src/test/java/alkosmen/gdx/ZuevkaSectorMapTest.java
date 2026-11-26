package alkosmen.gdx;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayDeque;
import javax.imageio.ImageIO;
import java.io.File;

/** Tests authored geometry in image coordinates, without a graphics context. */
public final class ZuevkaSectorMapTest {
    public static void main(String[] args) throws Exception {
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
        assert canReach(map, 800, 590, 1140, 440) : "No route from the yard entrance to the garden gnome";
        assert canReach(map, 870, 570, 1140, 440, 5f) : "Route from beside the stump has no clearance";
        assert !at(map, 720, 560) : "Removed entrance bucket still blocks the path";
        assert !at(map, 830, 580) : "Fence post blocks the passage above its base";
        assert canReach(map, 800, 590, 830, 580) : "Cannot pass the fence post from the entrance";
        for (int i = 0; i < map.colliders().size(); i++) {
            assert !java.util.Set.of("yard_table", "yard_stool", "yard_planter",
                "yard_stump", "porch_woodpile", "barrel_at_porch").contains(map.colliderName(i))
                : "Removed yard object still blocks";
        }
        assert at(map, 650, 310) : "Porch roof should not be walkable";
        assert map.occluders().stream().noneMatch(o -> o.id().equals("porch_roof") || o.id().equals("house_roof"))
            : "Outdated roof masks obscure the player on the cleaned porch";
        assert at(map, 850, 220) : "House roof should not be walkable";
        assert at(map, 246, 690) : "Sign post base should block";
        assert !at(map, 310, 660) : "Space beside the sign should be open";
        assert at(map, 820, 390) : "House should block";
        assert at(map, 1120, 365) : "Woodpile should block";
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
        assert map.yardFlowers().size() == 2 : "Missing yard flower locations";
        for (ZuevkaSectorMap.Flower flower : map.yardFlowers()) {
            assert flower.bounds().x >= 0 && flower.bounds().y >= 0;
            assert flower.bounds().x + flower.bounds().width <= map.width;
            assert flower.bounds().y + flower.bounds().height <= map.height;
            assert flower.bounds().contains(flower.x(), flower.y()) : "Flower anchor outside its bounds";
        }
        assert map.cleanedRegions().size() == 3 : "Expected local flower, bottle and bucket cleanups";
        assert map.sprites().isEmpty() : "Cleared yard should not show bottles";
        assert map.yardClearRegions().size() == 7;
        JsonValue savedLayout = new JsonReader().parse(new FileHandle(
            "src/main/resources/" + ZuevkaSectorMap.BASE + "objects.json"));
        for (JsonValue bottle : savedLayout.get("sprites")) bottle.get("enabled").set(true);
        ZuevkaSectorMap bottleMap = new ZuevkaSectorMap(savedLayout);
        assert bottleMap.sprites().size() == 2 : "Saved bottle positions cannot be restored";
        var cleaned = ImageIO.read(new File("src/main/resources/" + ZuevkaSectorMap.BASE
            + "yard_cleaned_reference.png"));
        var bottles = ImageIO.read(new File("src/main/resources/alkosmen/images/objects/bottle/bottle_tich_table.png"));
        var noBucket = ImageIO.read(new File("src/main/resources/" + ZuevkaSectorMap.BASE
            + "yard_no_bucket_reference.png"));
        var emptyYard = ImageIO.read(new File("src/main/resources/" + ZuevkaSectorMap.BASE
            + "yard_empty_reference.png"));
        assert Math.abs(cleaned.getWidth() - map.width) <= 1 && cleaned.getHeight() == map.height;
        assert noBucket.getWidth() == map.width && noBucket.getHeight() == map.height;
        assert emptyYard.getWidth() == map.width && emptyYard.getHeight() == map.height;
        assert bottles.getColorModel().hasAlpha() : "Bottle atlas needs transparency";
        for (ZuevkaSectorMap.SceneBottle sprite : bottleMap.sprites()) {
            assert sprite.width() > 0 && sprite.height() > 0;
            assert sprite.sourceX() >= 0 && sprite.sourceY() >= 0;
            assert sprite.sourceX() + sprite.sourceWidth() <= bottles.getWidth();
            assert sprite.sourceY() + sprite.sourceHeight() <= bottles.getHeight();
            assert sprite.inFrontOf(sprite.y() + 1f);
            assert !sprite.inFrontOf(sprite.y() - 1f);
            assert sprite.x() >= 0 && sprite.y() >= 0;
            assert sprite.x() + sprite.width() <= map.width;
            assert sprite.y() + sprite.height() <= map.height;
        }
        assert bottleMap.collectNearestBottle(0, 0) == null : "Cannot pick up remotely";
        assert canReach(map, 660, 745, 780, 535) : "Cannot reach the bottle pickup area";
        ZuevkaSectorMap.SceneBottle first = bottleMap.collectNearestBottle(780, map.height - 535);
        assert first != null && first.id().equals("table_bottle_left") : "Nearest bottle should be picked up";
        assert first.pickup().collected() && bottleMap.collectedBottleCount() == 1;
        ZuevkaSectorMap.SceneBottle second = bottleMap.sprites().get(1);
        assert canReach(map, 660, 745, 805, 525) : "Cannot reach the second bottle";
        assert bottleMap.collectNearestBottle(805, map.height - 525) == second;
        assert bottleMap.collectedBottleCount() == 2;
        assert bottleMap.collectNearestBottle(second.pickup().x(), second.pickup().y()) == null : "Collected twice";
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
        return canReach(map, startX, startY, targetX, targetY, 0f);
    }

    private static boolean canReach(ZuevkaSectorMap map, int startX, int startY,
                                    int targetX, int targetY, float margin) {
        int step = 5;
        int width = (int) map.width / step + 1;
        int height = (int) map.height / step + 1;
        Rectangle startFootprint = AlkosmenPlayer.footprint(startX, map.height - startY);
        startFootprint.set(startFootprint.x - margin, startFootprint.y - margin,
            startFootprint.width + margin * 2, startFootprint.height + margin * 2);
        if (map.blocks(startFootprint)) return false;
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
                    && !visited[nextX][nextY] && !blocksWithMargin(map, nextX * step, nextY * step, margin)) {
                    visited[nextX][nextY] = true;
                    queue.add(nextY * width + nextX);
                }
            }
        }
        return false;
    }

    private static boolean blocksWithMargin(ZuevkaSectorMap map, float imageX, float imageY, float margin) {
        Rectangle feet = AlkosmenPlayer.footprint(imageX, map.height - imageY);
        feet.set(feet.x - margin, feet.y - margin, feet.width + margin * 2, feet.height + margin * 2);
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
