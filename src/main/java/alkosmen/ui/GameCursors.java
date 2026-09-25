package alkosmen.ui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.EnumMap;
import javax.imageio.ImageIO;

public final class GameCursors {
   public enum Kind {
      NORMAL("cursor_normal.png"),
      QUEST_NEW("cursor_quest_new.png"),
      QUEST_TURNIN("cursor_quest_turnin.png"),
      INTERACT("cursor_interact.png");

      private final String fileName;

      Kind(String fileName) {
         this.fileName = fileName;
      }
   }

   private static final String RESOURCE_ROOT = "/alkosmen/ui/cursors/";
   private final EnumMap<Kind, Cursor> cursors = new EnumMap<>(Kind.class);
   private Kind active;

   public GameCursors() {
      for (Kind kind : Kind.values()) {
         cursors.put(kind, load(kind));
      }
   }

   public void apply(Component component, Kind kind) {
      if (active != kind) {
         component.setCursor(cursors.get(kind));
         active = kind;
      }
   }

   private static Cursor load(Kind kind) {
      URL resource = GameCursors.class.getResource(RESOURCE_ROOT + kind.fileName);
      if (resource == null) {
         throw new IllegalStateException("Cursor resource not found: " + kind.fileName);
      }
      try {
         BufferedImage image = ImageIO.read(resource);
         if (image == null) {
            throw new IllegalStateException("Invalid cursor image: " + kind.fileName);
         }
         return Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(3, 3), kind.name());
      } catch (IOException error) {
         throw new IllegalStateException("Could not load cursor: " + kind.fileName, error);
      } catch (HeadlessException | IllegalArgumentException error) {
         int fallback = kind == Kind.NORMAL ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR;
         return Cursor.getPredefinedCursor(fallback);
      }
   }
}
