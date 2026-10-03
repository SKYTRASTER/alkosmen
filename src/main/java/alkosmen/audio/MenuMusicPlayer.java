package alkosmen.audio;

import java.net.URL;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public final class MenuMusicPlayer {
   private static final String[] TRACKS = new String[]{"/alkosmen/sounds/menu/night_training_1.mp3", "/alkosmen/sounds/menu/night_training_2.mp3"};
   private volatile boolean enabled;
   private volatile long playbackPositionMs = -1L;
   private volatile int playingTrackIndex = -1;
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

   public long getPlaybackPositionMs() {
      return this.playbackPositionMs;
   }

   public int getCurrentTrackIndex() {
      return this.playingTrackIndex;
   }

   private void playNext() {
      if (this.enabled) {
         this.disposeCurrent();
         int nextTrackIndex = this.trackIndex;
         URL resource = MenuMusicPlayer.class.getResource(TRACKS[nextTrackIndex]);
         if (resource == null) {
            System.err.println("Menu track not found: " + TRACKS[nextTrackIndex]);
         } else {
            try {
               MediaPlayer player = new MediaPlayer(new Media(resource.toExternalForm()));
               this.current = player;
               this.playingTrackIndex = nextTrackIndex;
               this.playbackPositionMs = 0L;

               player.setVolume(0.65);
               player.currentTimeProperty().addListener((observable, oldValue, newValue) ->
                  this.playbackPositionMs = Math.max(0L, Math.round(newValue.toMillis()))
               );
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

      this.playbackPositionMs = -1L;
      this.playingTrackIndex = -1;
   }
}
