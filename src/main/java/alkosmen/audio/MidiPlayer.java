package alkosmen.audio;

import javax.sound.midi.*;
import java.io.IOException;
import java.io.InputStream;

public class MidiPlayer {
    private Sequencer sequencer;

    public void playLoop(String resourcePath) {
        stop(); // если уже что-то играет

        try {
            sequencer = MidiSystem.getSequencer();
            if (sequencer == null) throw new IllegalStateException("No MIDI sequencer available");

            sequencer.open();

            try (InputStream in = MidiPlayer.class.getResourceAsStream(resourcePath)) {
                if (in == null) throw new IOException("MIDI not found: " + resourcePath);
                Sequence seq = MidiSystem.getSequence(in);
                sequencer.setSequence(seq);
            }

            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();

        } catch (Exception e) {
            System.err.println("MIDI play error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            if (sequencer != null) {
                sequencer.stop();
                sequencer.close();
                sequencer = null;
            }
        } catch (Exception ignored) {}
    }
}