package alkosmen.audio;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;
import java.io.IOException;
import java.io.InputStream;

public class MidiPlayer {
    private Sequencer sequencer;
    private Synthesizer synthesizer;
    private Transmitter transmitter;

    public void playLoop(String resourcePath) {
        playLoop(resourcePath, 127);
    }

    public void playLoop(String resourcePath, int volume) {
        stop();

        try {
            int safeVolume = Math.max(0, Math.min(127, volume));

            sequencer = MidiSystem.getSequencer(false);
            if (sequencer == null) {
                throw new IllegalStateException("No MIDI sequencer available");
            }

            synthesizer = MidiSystem.getSynthesizer();
            if (synthesizer == null) {
                throw new IllegalStateException("No MIDI synthesizer available");
            }

            synthesizer.open();
            sequencer.open();
            transmitter = sequencer.getTransmitter();
            transmitter.setReceiver(synthesizer.getReceiver());

            try (InputStream in = MidiPlayer.class.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    throw new IOException("MIDI not found: " + resourcePath);
                }
                Sequence seq = MidiSystem.getSequence(in);
                sequencer.setSequence(seq);
            }

            for (int ch = 0; ch < 16; ch++) {
                ShortMessage msg = new ShortMessage();
                msg.setMessage(ShortMessage.CONTROL_CHANGE, ch, 7, safeVolume);
                synthesizer.getReceiver().send(msg, -1);
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
            if (transmitter != null) {
                transmitter.close();
                transmitter = null;
            }
            if (sequencer != null) {
                sequencer.stop();
                sequencer.close();
                sequencer = null;
            }
            if (synthesizer != null) {
                synthesizer.close();
                synthesizer = null;
            }
        } catch (Exception ignored) {
        }
    }
}
