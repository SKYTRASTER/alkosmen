package alkosmen.audio;

import java.net.URL;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public final class MenuMusicPlayer {
   private static final String[] TRACKS = new String[]{"/alkosmen/sounds/menu/night_training_1.mp3", "/alkosmen/sounds/menu/night_training_2.mp3"};
   private volatile boolean enabled;
   private MediaPlayer current;
   private int trackIndex;

   public MenuMusicPlayer() {
      try {
         Platform.startup(() -> {
            Platform.setImplicitExit(false);
            if (this.enabled) {
               this.playNext();
            }

         });
      } catch (IllegalStateException var2) {
         Platform.setImplicitExit(false);
      } catch (RuntimeException error) {
         System.err.println("Menu audio unavailable: " + error.getMessage());
      }

   }

   public void playLoop() {
      this.enabled = true;

      try {
         Platform.runLater(() -> {
            if (this.enabled && this.current == null) {
               this.playNext();
            }

         });
      } catch (IllegalStateException error) {
         System.err.println("Menu audio unavailable: " + error.getMessage());
      }

   }

   public void stop() {
      this.enabled = false;

      try {
         Platform.runLater(this::disposeCurrent);
      } catch (IllegalStateException var2) {
      }

   }

   private void playNext() {
      if (this.enabled) {
         this.disposeCurrent();
         URL resource = MenuMusicPlayer.class.getResource(TRACKS[this.trackIndex]);
         if (resource == null) {
            String var10001 = TRACKS[this.trackIndex];
            System.err.println("Menu track not found: " + var10001);
         } else {
            try {
               MediaPlayer player = new MediaPlayer(new Media(resource.toExternalForm()));
               this.current = player;
               player.setVolume(0.65);
               player.setOnEndOfMedia(() -> {
                  this.trackIndex = (this.trackIndex + 1) % TRACKS.length;
                  this.playNext();
               });
               player.setOnError(() -> {
                  System.err.println("Menu audio error: " + String.valueOf(player.getError()));
                  if (this.current == player) {
                     this.disposeCurrent();
                  }

               });
               player.play();
            } catch (RuntimeException error) {
               System.err.println("Menu audio error: " + error.getMessage());
               this.disposeCurrent();
            }

         }
      }
   }

   private void disposeCurrent() {
      if (this.current != null) {
         this.current.stop();
         this.current.dispose();
         this.current = null;
      }

   }
}
