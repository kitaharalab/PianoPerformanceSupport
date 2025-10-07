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
        String inputPath = "C:\\Users\\songo\\PianoPerformanceSupport\\pianoroll-sample\\src\\main\\resources\\kirakira2.mid";
        String outputPath = "C:\\Users\\songo\\PianoPerformanceSupport\\pianoroll-sample\\src\\main\\resources\\kirakira2_first6.mid";

        try {
            System.out.println("1. start");
            Sequence inputSequence = MidiSystem.getSequence(new File(inputPath));
            System.out.println("2. input loaded");

            Sequence outputSequence = new Sequence(inputSequence.getDivisionType(), inputSequence.getResolution());
            Track outputTrack = outputSequence.createTrack();
            System.out.println("3. output sequence created");

            int noteCount = 0;
            Track[] tracks = inputSequence.getTracks();

            outer: for (Track track : tracks) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage msg = event.getMessage();

                    if (msg instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) msg;
                        int cmd = sm.getCommand();
                        int vel = sm.getData2();

                        // NOTE_ON (vel>0) または NOTE_OFF (vel=0のNOTE_ONも含む)
                        if ((cmd == ShortMessage.NOTE_ON && vel > 0) ||
                            (cmd == ShortMessage.NOTE_ON && vel == 0) ||
                            (cmd == ShortMessage.NOTE_OFF)) {

                            // ShortMessage をコピーして追加
                            ShortMessage copy = new ShortMessage();
                            copy.setMessage(cmd, sm.getChannel(), sm.getData1(), vel);
                            outputTrack.add(new MidiEvent(copy, event.getTick()));

                            // NOTE_ON のカウント（コピー後に増やす）
                            if (cmd == ShortMessage.NOTE_ON && vel > 0) {
                                noteCount++;
                                if (noteCount >= 7) break outer;
                            }
                        }
                    }
                }
            }
            System.out.println("4. loop finished");

            //トラック終端イベントを追加
            MetaMessage endOfTrack = new MetaMessage();
            endOfTrack.setMessage(0x2F, new byte[0], 0);
            outputTrack.add(new MidiEvent(endOfTrack, outputSequence.getTickLength()));
            System.out.println("5. end of track added");

            // 保存
            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs(); // フォルダがなければ作成
            int result = MidiSystem.write(outputSequence, 1, outputFile);
            System.out.println("6. Written bytes: " + result);
            System.out.println("7. Saved first 4 notes to: " + outputPath);

        } catch (IOException | InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }
}
