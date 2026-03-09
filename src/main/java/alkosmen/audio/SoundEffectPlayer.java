package alkosmen.audio;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

public final class SoundEffectPlayer {
    private final Clip clip;

    public SoundEffectPlayer(String resourcePath) {
        try {
            URL url = SoundEffectPlayer.class.getResource(resourcePath);
            if (url == null) {
                throw new IllegalArgumentException("Sound resource not found: " + resourcePath);
            }

            try (AudioInputStream original = AudioSystem.getAudioInputStream(url);
                 AudioInputStream pcmStream = toPcm16(original)) {

                AudioFormat fmt = pcmStream.getFormat();
                DataLine.Info info = new DataLine.Info(Clip.class, fmt);

                if (!AudioSystem.isLineSupported(info)) {
                    throw new LineUnavailableException("Clip line not supported for format: " + fmt);
                }

                clip = (Clip) AudioSystem.getLine(info);
                clip.open(pcmStream); // loads into memory
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load sound effect: " + resourcePath, e);
        }
    }

    private static AudioInputStream toPcm16(AudioInputStream in) throws IOException {
        AudioFormat src = in.getFormat();

        float sr = (src.getSampleRate() > 0 ? src.getSampleRate() : 44100f);
        int ch = (src.getChannels() > 0 ? src.getChannels() : 2);

        AudioFormat pcm16 = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sr,
                16,
                ch,
                ch * 2,   // frameSize
                sr,
                false     // little-endian
        );

        if (AudioSystem.isConversionSupported(pcm16, src)) {
            return AudioSystem.getAudioInputStream(pcm16, in);
        }

        // Fallback: try 44100 Hz if the converter/mixer is picky
        AudioFormat pcm16_441 = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100f, 16, ch, ch * 2, 44100f, false
        );

        if (AudioSystem.isConversionSupported(pcm16_441, src)) {
            return AudioSystem.getAudioInputStream(pcm16_441, in);
        }

        // No conversion path available; return original (will likely fail, but with clearer context)
        return in;
    }

    public void play() {
        if (clip.isRunning()) clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    public void stop() {
        if (clip.isRunning()) {
            clip.stop();
        }
        clip.setFramePosition(0);
    }
}
