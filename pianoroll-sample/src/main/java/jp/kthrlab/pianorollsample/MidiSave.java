package jp.kthrlab.pianorollsample;

import java.io.File;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

public class MidiSave {
    public static void main(String[] args) {
        String inputPath = "C:\\Users\\songo\\PianoPerformanceSupport\\pianoroll-sample\\src\\main\\resources\\ex1.mid";
        String outputPath = "C:\\Users\\songo\\PianoPerformanceSupport\\pianoroll-sample\\src\\main\\resources\\ex1_0to8.mid";

        try {
            Sequence inputSequence = MidiSystem.getSequence(new File(inputPath));

            Sequence outputSequence = new Sequence(inputSequence.getDivisionType(), inputSequence.getResolution());
            Track outputTrack = outputSequence.createTrack();

            int noteCount = 0;
            Track[] tracks = inputSequence.getTracks();

            int startNote = 1;
            int endNote = 9;

            long baseTick = -1; 

            outer: for (Track track : tracks) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage msg = event.getMessage();

                    if (msg instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) msg;
                        int cmd = sm.getCommand();
                        int vel = sm.getData2();
                        long tick = event.getTick();

                        boolean isNoteOn = (cmd == ShortMessage.NOTE_ON && vel > 0);
                        boolean isNoteOff = (cmd == ShortMessage.NOTE_OFF) ||
                                (cmd == ShortMessage.NOTE_ON && vel == 0);

                        if (isNoteOn || isNoteOff) {

                            if (isNoteOn) {
                                noteCount++;
                            }

                            if (noteCount < startNote)
                                continue;

                            if (noteCount > endNote)
                                break outer;

                            if (baseTick < 0 && isNoteOn) {
                                baseTick = tick;
                            }

                            long newTick = tick - baseTick;

                            ShortMessage copy = new ShortMessage();
                            copy.setMessage(cmd, sm.getChannel(), sm.getData1(), vel);
                            outputTrack.add(new MidiEvent(copy, newTick));
                        }
                    }
                }
            }

            // トラック終端イベントを追加
            MetaMessage endOfTrack = new MetaMessage();
            endOfTrack.setMessage(0x2F, new byte[0], 0);
            outputTrack.add(new MidiEvent(endOfTrack, outputSequence.getTickLength()));

            // 保存
            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs(); // フォルダがなければ作成
            int result = MidiSystem.write(outputSequence, 1, outputFile);

        } catch (IOException | InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }
}