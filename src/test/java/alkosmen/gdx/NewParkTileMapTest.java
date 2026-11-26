package alkosmen.gdx;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.math.Rectangle;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Checks the real sector coordinates with a texture-free copy of the TMX geometry. */
public final class NewParkTileMapTest {
    public static void main(String[] args) throws Exception {
        Element map;
        try (InputStream input = NewParkTileMapTest.class.getClassLoader()
            .getResourceAsStream(NewParkTileMap.MAP_PATH)) {
            map = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(input).getDocumentElement();
        }
        int width = Integer.parseInt(map.getAttribute("width"));
        int height = Integer.parseInt(map.getAttribute("height"));
        TiledMapTileLayer paths = new TiledMapTileLayer(width, height, 16, 16);
        Map<Integer, StaticTiledMapTile> tiles = new HashMap<>();
        NodeList tileDefinitions = map.getElementsByTagName("tile");
        for (int i = 0; i < tileDefinitions.getLength(); i++) {
            Element definition = (Element) tileDefinitions.item(i);
            StaticTiledMapTile tile = new StaticTiledMapTile(new TextureRegion());
            Element property = (Element) definition.getElementsByTagName("property").item(0);
            if (property.getAttribute("name").equals("walkable")) {
                tile.getProperties().put("walkable", Boolean.valueOf(property.getAttribute("value")));
            }
            // The fixture has one tileset with firstgid=1.
            tiles.put(Integer.parseInt(definition.getAttribute("id")) + 1, tile);
        }
        NodeList layers = map.getElementsByTagName("layer");
        for (int i = 0; i < layers.getLength(); i++) {
            Element layer = (Element) layers.item(i);
            if (!layer.getAttribute("name").equals("paths")) continue;
            String[] ids = layer.getElementsByTagName("data").item(0).getTextContent().trim()
                .split("\\s*,\\s*");
            for (int j = 0; j < ids.length; j++) {
                int id = Integer.parseInt(ids[j]);
                if (id != 0) {
                    StaticTiledMapTile tile = tiles.computeIfAbsent(id,
                        ignored -> new StaticTiledMapTile(new TextureRegion()));
                    paths.setCell(j % width, height - 1 - j / width,
                        new TiledMapTileLayer.Cell().setTile(tile));
                }
            }
        }
        List<Rectangle> solids = new ArrayList<>();
        NodeList objects = map.getElementsByTagName("object");
        for (int i = 0; i < objects.getLength(); i++) {
            Element object = (Element) objects.item(i);
            float x = Float.parseFloat(object.getAttribute("x"));
            float y = Float.parseFloat(object.getAttribute("y"));
            float w = Float.parseFloat(object.getAttribute("width"));
            float h = Float.parseFloat(object.getAttribute("height"));
            solids.add(NewParkTileMap.worldRectangle(new Rectangle(x, height * 16 - y - h, w, h)));
        }

        assert !blocks(paths, solids, NewParkTileGame.SPAWN_X, NewParkTileGame.SPAWN_Y) : "Spawn blocked";
        for (int x = 1625; x <= 1732; x++) {
            assert !blocks(paths, solids, x, 425f) : "Cannot walk to the precise lawn edge";
        }
        assert blocks(paths, solids, Math.nextUp(1732f), 425f) : "Feet overlap the lawn";
        assert blocks(paths, solids, 1775f, 425f) : "Lawn is walkable";
        for (int y = 175; y <= 575; y++) {
            assert !blocks(paths, solids, 1625f, y) : "Road / curb / path transition blocked";
        }
        for (int y = 925; y < 1325; y++) {
            assert !blocks(paths, solids, 425f, y) : "Outer path is blocked";
        }
        assert !blocks(paths, solids, 1375f, 1050f) : "Plaza blocked away from fountain";
        assert blocks(paths, solids, 1625f, 975f) : "Player can stand inside fountain";
        assert blocks(paths, solids, 10f, 175f) : "Outside map is walkable";

        paths.setCell(35, 8, new TiledMapTileLayer.Cell().setTile(new StaticTiledMapTile(new TextureRegion())));
        assert blocks(paths, solids, 1775f, 425f) : "Unmarked path-layer decoration is walkable";
        assert NewParkTileMap.coversHero(376.25f, 380f) : "Tree behind the hero's feet is drawn first";
        assert !NewParkTileMap.coversHero(376.25f, 375f) : "Tree covers the hero south of its trunk";
        System.out.println("NewParkTileMap checks passed");
    }

    private static boolean blocks(TiledMapTileLayer paths, List<Rectangle> solids, float x, float y) {
        return NewParkTileMap.blocks(paths, solids, AlkosmenPlayer.footprint(x, y));
    }
}
