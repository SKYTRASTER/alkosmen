package alkosmen.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Read-only metadata from the New Park source map. World geometry deliberately
 * stays elsewhere, so changing a display name never moves a road in-game.
 */
public final class NewParkMapLayout {
    public static final String RESOURCE_PATH = "alkosmen/maps/new_park/map-layout.json";

    private final String city;
    private final List<Road> roads;

    private NewParkMapLayout(String city, List<Road> roads) {
        this.city = city;
        this.roads = List.copyOf(roads);
    }

    public static NewParkMapLayout load() {
        FileHandle resource = Gdx.files.internal(RESOURCE_PATH);
        if (!resource.exists()) {
            throw new IllegalStateException("New Park layout was not found: " + RESOURCE_PATH);
        }

        JsonValue root = new JsonReader().parse(resource);
        String city = requiredText(root, "city");
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
            roads.add(new Road(id, name));
        }
        return new NewParkMapLayout(city, roads);
    }

    public String city() {
        return city;
    }

    public List<Road> roads() {
        return roads;
    }

    private static String requiredText(JsonValue value, String field) {
        String text = value.getString(field, "").trim();
        if (text.isEmpty()) {
            throw new IllegalStateException("New Park layout requires a non-empty " + field);
        }
        return text;
    }

    public record Road(String id, String name) {
    }
}

