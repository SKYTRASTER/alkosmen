package alkosmen.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Data from the traced reference map; display names stay in JSON. */
public final class NewParkMapLayout {
    public static final String RESOURCE_PATH = "alkosmen/maps/new_park/map-layout.json";

    private final String city;
    private final int imageWidth;
    private final int imageHeight;
    private final List<Road> roads;
    private final List<List<MapPoint>> railwayLines;

    private NewParkMapLayout(
        String city, int imageWidth, int imageHeight,
        List<Road> roads, List<List<MapPoint>> railwayLines
    ) {
        this.city = city;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.roads = List.copyOf(roads);
        this.railwayLines = List.copyOf(railwayLines);
    }

    public static NewParkMapLayout load() {
        FileHandle resource = Gdx.files.internal(RESOURCE_PATH);
        if (!resource.exists()) {
            throw new IllegalStateException("New Park layout was not found: " + RESOURCE_PATH);
        }

        JsonValue root = new JsonReader().parse(resource);
        String city = requiredText(root, "city");
        JsonValue source = root.get("source");
        if (source == null) {
            throw new IllegalStateException("New Park layout requires source dimensions");
        }
        int imageWidth = source.getInt("imageWidth");
        int imageHeight = source.getInt("imageHeight");
        if (imageWidth <= 0 || imageHeight <= 0) {
            throw new IllegalStateException("New Park source dimensions must be positive");
        }

        JsonValue roadsValue = root.get("roads");
        if (roadsValue == null || !roadsValue.isArray()) {
            throw new IllegalStateException("New Park layout must contain a roads array");
        }
        List<Road> roads = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (JsonValue road = roadsValue.child; road != null; road = road.next) {
            String id = requiredText(road, "id");
            String name = requiredText(road, "name");
            if (!ids.add(id)) {
                throw new IllegalStateException("Duplicate New Park road id: " + id);
            }
            float width = road.getFloat("widthPx");
            if (width <= 0f) {
                throw new IllegalStateException("Road width must be positive: " + id);
            }
            roads.add(new Road(id, name, readLine(road.get("centerline"), id), width));
        }

        JsonValue railway = root.get("railway");
        List<List<MapPoint>> railwayLines = new ArrayList<>();
        if (railway != null) {
            JsonValue lines = railway.get("lines");
            if (lines == null || !lines.isArray()) {
                throw new IllegalStateException("New Park railway requires lines");
            }
            for (JsonValue line = lines.child; line != null; line = line.next) {
                railwayLines.add(readLine(line, "railway"));
            }
        }
        return new NewParkMapLayout(city, imageWidth, imageHeight, roads, railwayLines);
    }

    private static List<MapPoint> readLine(JsonValue values, String id) {
        if (values == null || !values.isArray() || values.size < 2) {
            throw new IllegalStateException("Map line needs at least two points: " + id);
        }
        List<MapPoint> points = new ArrayList<>();
        for (JsonValue point = values.child; point != null; point = point.next) {
            if (!point.isArray() || point.size != 2) {
                throw new IllegalStateException("Invalid map point in " + id);
            }
            points.add(new MapPoint(point.get(0).asFloat(), point.get(1).asFloat()));
        }
        return List.copyOf(points);
    }

    private static String requiredText(JsonValue value, String field) {
        String text = value.getString(field, "").trim();
        if (text.isEmpty()) {
            throw new IllegalStateException("New Park layout requires a non-empty " + field);
        }
        return text;
    }

    public String city() {
        return city;
    }

    public int imageWidth() {
        return imageWidth;
    }

    public int imageHeight() {
        return imageHeight;
    }

    public List<Road> roads() {
        return roads;
    }

    public List<List<MapPoint>> railwayLines() {
        return railwayLines;
    }

    public record MapPoint(float x, float y) {
    }

    public record Road(String id, String name, List<MapPoint> centerline, float widthPx) {
        public Road {
            centerline = List.copyOf(centerline);
        }
    }
}
