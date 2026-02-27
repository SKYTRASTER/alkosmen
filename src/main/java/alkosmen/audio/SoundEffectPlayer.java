package alkosmen.audio;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.net.URL;

public final class SoundEffectPlayer {
    private final Clip clip;

    public SoundEffectPlayer(String resourcePath) {
        try {
            URL url = SoundEffectPlayer.class.getResource(resourcePath);
            if (url == null) {
                throw new IllegalArgumentException("Sound resource not found: " + resourcePath);
            }

            AudioInputStream stream = AudioSystem.getAudioInputStream(url);
            clip = AudioSystem.getClip();
            clip.open(stream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load sound effect: " + resourcePath, e);
        }
    }

    public void play() {
        if (clip.isRunning()) {
            clip.stop();
        }
        clip.setFramePosition(0);
        clip.start();
    }
}
