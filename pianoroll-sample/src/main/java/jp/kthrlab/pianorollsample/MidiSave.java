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
        String outputPath = "C:\\Users\\songo\\PianoPerformanceSupport\\pianoroll-sample\\src\\main\\resources\\kirakira2_5_7.mid";

        try {
            System.out.println("1. start");
            Sequence inputSequence = MidiSystem.getSequence(new File(inputPath));
            System.out.println("2. input loaded");

            Sequence outputSequence = new Sequence(inputSequence.getDivisionType(), inputSequence.getResolution());
            Track outputTrack = outputSequence.createTrack();
            System.out.println("3. output sequence created");

            int noteCount = 0;
            Track[] tracks = inputSequence.getTracks();

            // int startNote = 1; // ここから開始
            // int endNote = 7; // ここまで切り取る
            //
            // outer: for (Track track : tracks) {
            // for (int i = 0; i < track.size(); i++) {
            // MidiEvent event = track.get(i);
            // MidiMessage msg = event.getMessage();
            //
            // if (msg instanceof ShortMessage) {
            // ShortMessage sm = (ShortMessage) msg;
            // int cmd = sm.getCommand();
            // int vel = sm.getData2();
            //
            // if ((cmd == ShortMessage.NOTE_ON && vel > 0) ||
            // (cmd == ShortMessage.NOTE_ON && vel == 0) ||
            // (cmd == ShortMessage.NOTE_OFF)) {
            //
            // // NOTE_ON の場合に noteCount を増加
            // if (cmd == ShortMessage.NOTE_ON && vel > 0) {
            // noteCount++;
            // }
            //
            // // startNote より前 → スキップ
            // if (noteCount < startNote)
            // continue;
            //
            // // endNote を超えたら終了
            // if (noteCount > endNote)
            // break outer;
            //
            // // startNote〜endNote の範囲だけコピーする
            // ShortMessage copy = new ShortMessage();
            // copy.setMessage(cmd, sm.getChannel(), sm.getData1(), vel);
            // outputTrack.add(new MidiEvent(copy, event.getTick()));
            // }
            // }
            // }
            // }

            int startNote = 5;
            int endNote = 7;

            long baseTick = -1; // 5音目のNOTE_ON tick を記録する

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

                            // NOTE_ON のたび +1
                            if (isNoteOn) {
                                noteCount++;
                            }

                            // 5 音目より前 → 無視
                            if (noteCount < startNote)
                                continue;

                            // 8音目以降 → 終了
                            if (noteCount > endNote)
                                break outer;

                            // ★ ここで基準 tick を決める ★
                            if (baseTick < 0 && isNoteOn) {
                                baseTick = tick; // 5 音目の最初の tick を記録
                            }

                            // 新しい tick = （元 tick - baseTick）
                            long newTick = tick - baseTick;

                            // コピーして追加
                            ShortMessage copy = new ShortMessage();
                            copy.setMessage(cmd, sm.getChannel(), sm.getData1(), vel);
                            outputTrack.add(new MidiEvent(copy, newTick));
                        }
                    }
                }
            }

            //// トラック終端
            //MetaMessage endOfTrack = new MetaMessage();
            //endOfTrack.setMessage(0x2F, new byte[0], 0);
            //outputTrack.add(new MidiEvent(endOfTrack, outputSequence.getTickLength()));

            System.out.println("4. loop finished");

            // トラック終端イベントを追加
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
