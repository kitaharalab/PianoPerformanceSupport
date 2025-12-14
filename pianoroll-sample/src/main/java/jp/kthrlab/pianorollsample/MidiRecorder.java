package jp.kthrlab.pianorollsample;

import java.io.File;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

public class MidiRecorder {

    private static final int PPQ = 480;
    private Sequence sequence;
    private Track track;
    private long startTimeMillis;

    public MidiRecorder() {
        try {
            sequence = new Sequence(Sequence.PPQ, PPQ);
            track = sequence.createTrack();
            startTimeMillis = System.currentTimeMillis();

            // テンポ設定（120 BPM）
            MetaMessage tempo = new MetaMessage();
            int mpq = 500000; // 120 BPM
            byte[] data = {
                    (byte) ((mpq >> 16) & 0xFF),
                    (byte) ((mpq >> 8) & 0xFF),
                    (byte) (mpq & 0xFF)
            };
            tempo.setMessage(0x51, data, 3);
            track.add(new MidiEvent(tempo, 0));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long nowTick() {
        long elapsed = System.currentTimeMillis() - startTimeMillis;
        return elapsed * PPQ * 120 / 60 / 1000;
    }

    public void reset() {
        try {
        sequence = new Sequence(Sequence.PPQ, PPQ);
        track = sequence.createTrack();
        startTimeMillis = System.currentTimeMillis();

        // テンポ再設定
        MetaMessage tempo = new MetaMessage();
        int mpq = 500000; // 120 BPM
        byte[] data = {
            (byte)((mpq >> 16) & 0xFF),
            (byte)((mpq >> 8) & 0xFF),
            (byte)(mpq & 0xFF)
        };
        tempo.setMessage(0x51, data, 3);
        track.add(new MidiEvent(tempo, 0));

    } catch (Exception e) {
        e.printStackTrace();
    }
    }

    public void record(byte[] msg) {
        try {
            int status = msg[0] & 0xF0;
            int channel = msg[0] & 0x0F;

            if (status == 0x90 || status == 0x80) {
                ShortMessage sm = new ShortMessage();
                sm.setMessage(msg[0], msg[1], msg[2]);
                track.add(new MidiEvent(sm, nowTick()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save(String path) {
        try {
            MidiSystem.write(sequence, 1, new File(path));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
