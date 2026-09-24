package alkosmen.gfx;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

public final class CharacterSpriteAssets {
   public static final String ALKOSMEN_WALK_ATLAS = "/alkosmen/ui/sprites/alkosmen/walk_atlas_v1.png";
   private static final String ROOT = "/alkosmen/ui/sprites/";

   private CharacterSpriteAssets() {
   }

   public static String sheetPath(CharacterId character, Action action) {
      return "/alkosmen/ui/sprites/" + character.folder + "/" + action.file + ".png";
   }

   public static BufferedImage[] loadHorizontalSheet(CharacterId character, Action action, int frameWidth, int frameHeight) throws IOException {
      String path = sheetPath(character, action);
      URL resource = CharacterSpriteAssets.class.getResource(path);
      if (resource == null) {
         return new BufferedImage[0];
      } else {
         BufferedImage sheet = ImageIO.read(resource);
         if (sheet != null && frameWidth > 0 && frameHeight > 0 && sheet.getHeight() == frameHeight && sheet.getWidth() % frameWidth == 0) {
            int count = sheet.getWidth() / frameWidth;
            BufferedImage[] frames = new BufferedImage[count];

            for(int frame = 0; frame < count; ++frame) {
               frames[frame] = sheet.getSubimage(frame * frameWidth, 0, frameWidth, frameHeight);
            }

            return frames;
         } else {
            throw new IOException("Invalid sprite sheet geometry: " + path);
         }
      }
   }

   public static BufferedImage[][] loadGridAtlas(String path, int columns, int rows) throws IOException {
      return loadGridAtlas(path, columns, rows, 0);
   }

   public static BufferedImage[][] loadGridAtlas(String path, int columns, int rows, int bottomTrim) throws IOException {
      URL resource = CharacterSpriteAssets.class.getResource(path);
      if (resource == null) {
         throw new IOException("Sprite atlas not found: " + path);
      } else {
         BufferedImage atlas = ImageIO.read(resource);
         if (atlas != null && columns > 0 && rows > 0 && bottomTrim >= 0) {
            BufferedImage[][] frames = new BufferedImage[rows][columns];

            for(int row = 0; row < rows; ++row) {
               int top = row * atlas.getHeight() / rows;
               int bottom = (row + 1) * atlas.getHeight() / rows;

               for(int column = 0; column < columns; ++column) {
                  int left = column * atlas.getWidth() / columns;
                  int right = (column + 1) * atlas.getWidth() / columns;
                  int height = bottom - top - bottomTrim;
                  if (height <= 0) {
                     throw new IOException("Sprite atlas trim exceeds frame height: " + path);
                  }

                  frames[row][column] = atlas.getSubimage(left, top, right - left, height);
               }
            }

            return frames;
         } else {
            throw new IOException("Invalid sprite atlas: " + path);
         }
      }
   }

   public static enum CharacterId {
      ALKOSMEN("alkosmen"),
      TOLYA("tolya");

      private final String folder;

      private CharacterId(String folder) {
         this.folder = folder;
      }

      // $FF: synthetic method
      private static CharacterId[] $values() {
         return new CharacterId[]{ALKOSMEN, TOLYA};
      }
   }

   public static enum Action {
      IDLE("idle"),
      WALK_LEFT("walk_left"),
      WALK_RIGHT("walk_right"),
      WALK_UP("walk_up"),
      WALK_DOWN("walk_down"),
      LAUGH("laugh"),
      FALL("fall"),
      ACCORDION("accordion"),
      DANCE("dance");

      private final String file;

      private Action(String file) {
         this.file = file;
      }

      // $FF: synthetic method
      private static Action[] $values() {
         return new Action[]{IDLE, WALK_LEFT, WALK_RIGHT, WALK_UP, WALK_DOWN, LAUGH, FALL, ACCORDION, DANCE};
      }
   }
}
