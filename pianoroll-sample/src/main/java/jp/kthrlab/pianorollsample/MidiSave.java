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
            //System.out.println("1. start");
            Sequence inputSequence = MidiSystem.getSequence(new File(inputPath));
            //System.out.println("2. input loaded");

            Sequence outputSequence = new Sequence(inputSequence.getDivisionType(), inputSequence.getResolution());
            Track outputTrack = outputSequence.createTrack();
            //System.out.println("3. output sequence created");

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

            int startNote = 1;
            int endNote = 9;

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
            // MetaMessage endOfTrack = new MetaMessage();
            // endOfTrack.setMessage(0x2F, new byte[0], 0);
            // outputTrack.add(new MidiEvent(endOfTrack, outputSequence.getTickLength()));

            //System.out.println("4. loop finished");

            // トラック終端イベントを追加
            MetaMessage endOfTrack = new MetaMessage();
            endOfTrack.setMessage(0x2F, new byte[0], 0);
            outputTrack.add(new MidiEvent(endOfTrack, outputSequence.getTickLength()));
            //System.out.println("5. end of track added");

            // 保存
            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs(); // フォルダがなければ作成
            int result = MidiSystem.write(outputSequence, 1, outputFile);
            //System.out.println("6. Written bytes: " + result);
            //System.out.println("7. Saved first 4 notes to: " + outputPath);

        } catch (IOException | InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }
}

// package jp.kthrlab.pianorollsample;
//
// import java.io.File;
// import java.io.IOException;
// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.Comparator;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Set;
//
// import javax.sound.midi.InvalidMidiDataException;
// import javax.sound.midi.MetaMessage;
// import javax.sound.midi.MidiEvent;
// import javax.sound.midi.MidiMessage;
// import javax.sound.midi.MidiSystem;
// import javax.sound.midi.Sequence;
// import javax.sound.midi.ShortMessage;
// import javax.sound.midi.SysexMessage;
// import javax.sound.midi.Track;
//
// public class MidiSave {
// static class EventEntry {
// long tick;
// int trackIndex;
// int indexInTrack;
// MidiMessage msg;
//
// EventEntry(long tick, int trackIndex, int indexInTrack, MidiMessage msg) {
// this.tick = tick;
// this.trackIndex = trackIndex;
// this.indexInTrack = indexInTrack;
// this.msg = msg;
// }
// }
//
// public static void main(String[] args) {
// String inputPath =
// "C:\\Users\\songo\\PianoPerformanceSupport\\pianoroll-sample\\src\\main\\resources\\ex1.mid";
// String outputPath =
// "C:\\Users\\songo\\PianoPerformanceSupport\\pianoroll-sample\\src\\main\\resources\\ex1_0to8.mid";
//
// // 切り出す NOTE_ON の番号（1 始まり）
// int startNote = 1;
// int endNote = 9;
//
// try {
// Sequence inputSequence = MidiSystem.getSequence(new File(inputPath));
// Sequence outputSequence = new Sequence(inputSequence.getDivisionType(),
// inputSequence.getResolution());
// Track outputTrack = outputSequence.createTrack();
//
// // 全イベントを収集してソート（tick, trackIndex, indexInTrack）
// List<EventEntry> allEvents = new ArrayList<>();
// Track[] tracks = inputSequence.getTracks();
// for (int ti = 0; ti < tracks.length; ti++) {
// Track tr = tracks[ti];
// for (int i = 0; i < tr.size(); i++) {
// MidiEvent ev = tr.get(i);
// allEvents.add(new EventEntry(ev.getTick(), ti, i, ev.getMessage()));
// }
// }
// Collections.sort(allEvents, Comparator
// .comparingLong((EventEntry e) -> e.tick)
// .thenComparingInt(e -> e.trackIndex)
// .thenComparingInt(e -> e.indexInTrack));
//
// // 1回目のパス：どの NOTE_ON を選ぶか決めて、対応する NOTE_OFF も含めるための情報を作る
// int noteOnCount = 0;
// Set<Integer> selectedNoteKeys = new HashSet<>(); // channel<<8 | note ->
// currently selected
// // (音がONされたとき選ばれていたら保持)
// Set<Integer> everSelectedNotes = new HashSet<>(); // 選択した音符（ON
// のときに選ばれたノート番号キー）を記録（対応する OFF 抜け防止用）
// Long baseTickObj = null; // 最初に選んだ NOTE_ON の tick
//
// // ここでは逐次処理して、選択となる NOTE_ON が何であるか、そしてそのノート番号（チャンネル+note）を "選択中" とする。
// // NOTE_OFF は、そのノート番号が選択中であれば出力対象とする。
// for (EventEntry e : allEvents) {
// MidiMessage msg = e.msg;
// if (msg instanceof ShortMessage) {
// ShortMessage sm = (ShortMessage) msg;
// int cmd = sm.getCommand();
// int channel = sm.getChannel();
// int data1 = sm.getData1();
// int data2 = sm.getData2();
// boolean isNoteOn = (cmd == ShortMessage.NOTE_ON && data2 > 0);
// boolean isNoteOff = (cmd == ShortMessage.NOTE_OFF) || (cmd ==
// ShortMessage.NOTE_ON && data2 == 0);
// int key = (channel << 8) | (data1 & 0xff);
//
// if (isNoteOn) {
// noteOnCount++;
// if (noteOnCount >= startNote && noteOnCount <= endNote) {
// // これを選択
// selectedNoteKeys.add(key);
// everSelectedNotes.add(key);
// if (baseTickObj == null) {
// baseTickObj = e.tick; // 最初に選ばれた NOTE_ON の tick を base にする
// }
// }
// } else if (isNoteOff) {
// // NOTE_OFF は、そのノートが "選択された音の対応" であれば出力対象にする。
// if (selectedNoteKeys.contains(key)) {
// // そのノートの off を受けたら選択中から消す（ペアの完了）
// selectedNoteKeys.remove(key);
// } else {
// // もし everSelectedNotes に含まれていて、現在 selectedNoteKeys にない場合でも
// // NOTE_ON が start より前で NOTE_OFF が後に来るケース（前のノートの off）があるので、
// // everSelectedNotes に含まれていれば出力対象とする（安全策）。
// // 実運用ではここはさらに厳密化できる。
// }
// }
// } else if (msg instanceof MetaMessage) {
// // メタイベントはここでは特にスキップせず扱う（後で調整して追加）
// } else if (msg instanceof SysexMessage) {
// // 必要なら扱う（今回は後でコピー）
// }
// }
//
// if (baseTickObj == null) {
// System.out.println("指定した範囲内に NOTE_ON が見つかりませんでした。startNote/endNote
// を確認してください。");
// return;
// }
// long baseTick = baseTickObj;
//
// // 2回目のパス：選択判定を再実行して、出力用にイベントを追加（meta は base より前なら tick=0 にする）
// noteOnCount = 0;
// selectedNoteKeys.clear();
// long lastOutputTick = 0;
//
// for (EventEntry e : allEvents) {
// MidiMessage msg = e.msg;
// long origTick = e.tick;
// long newTick;
// if (origTick < baseTick)
// newTick = 0;
// else
// newTick = origTick - baseTick;
//
// if (msg instanceof ShortMessage) {
// ShortMessage sm = (ShortMessage) msg;
// int cmd = sm.getCommand();
// int channel = sm.getChannel();
// int data1 = sm.getData1();
// int data2 = sm.getData2();
// boolean isNoteOn = (cmd == ShortMessage.NOTE_ON && data2 > 0);
// boolean isNoteOff = (cmd == ShortMessage.NOTE_OFF) || (cmd ==
// ShortMessage.NOTE_ON && data2 == 0);
// int key = (channel << 8) | (data1 & 0xff);
//
// if (isNoteOn) {
// noteOnCount++;
// if (noteOnCount >= startNote && noteOnCount <= endNote) {
// // 選択対象 -> コピーして出力
// selectedNoteKeys.add(key);
// ShortMessage copy = new ShortMessage();
// copy.setMessage(cmd, channel, data1, data2);
// outputTrack.add(new MidiEvent(copy, newTick));
// lastOutputTick = Math.max(lastOutputTick, newTick);
// } else {
// // 範囲外の NOTE_ON はスキップ
// }
// } else if (isNoteOff) {
// // NOTE_OFF は、そのノートが選択されていた（選択中、または以前に選択された）ものだけ出力
// if (selectedNoteKeys.contains(key) || (/* 補助条件 */ false)) {
// ShortMessage copy = new ShortMessage();
// copy.setMessage(cmd, channel, data1, data2);
// outputTrack.add(new MidiEvent(copy, newTick));
// lastOutputTick = Math.max(lastOutputTick, newTick);
// // 選択中なら解除（ペア完了）
// selectedNoteKeys.remove(key);
// } else {
// // スキップ（範囲外のノートの off）
// }
// } else {
// // NOTE 以外の ShortMessage（ProgramChange など）は、必要ならコピーする
// // ここでは ProgramChange と ControlChange はコピーしておく（任意）
// int status = sm.getStatus() & 0xF0;
// if (status == ShortMessage.PROGRAM_CHANGE || status ==
// ShortMessage.CONTROL_CHANGE) {
// ShortMessage copy = new ShortMessage();
// copy.setMessage(sm.getStatus(), sm.getData1(), (sm.getLength() >= 3 ?
// sm.getData2() : 0));
// outputTrack.add(new MidiEvent(copy, newTick));
// lastOutputTick = Math.max(lastOutputTick, newTick);
// }
// }
// } else if (msg instanceof MetaMessage) {
// MetaMessage mm = (MetaMessage) msg;
// int type = mm.getType();
// byte[] data = mm.getData();
// // end-of-track は最後に自分で追加するので skip しておく
// if (type == 0x2F)
// continue;
// // tempo (0x51) などは base より前でも tick=0 に置くと再生に整合性が出ます
// long addTick = newTick;
// if (origTick < baseTick)
// addTick = 0;
// MetaMessage copy = new MetaMessage();
// copy.setMessage(type, data, data.length);
// outputTrack.add(new MidiEvent(copy, addTick));
// lastOutputTick = Math.max(lastOutputTick, addTick);
// } else if (msg instanceof SysexMessage) {
// SysexMessage sx = (SysexMessage) msg;
// byte[] data = sx.getData();
// SysexMessage copy = new SysexMessage();
// copy.setMessage(sx.getStatus(), data, data.length);
// outputTrack.add(new MidiEvent(copy, newTick));
// lastOutputTick = Math.max(lastOutputTick, newTick);
// } else {
// // その他のメッセージは無視 or 必要ならコピー
// }
// }
//
// // 最後に EndOfTrack を追加（最後のイベントより少し後ろ）
// MetaMessage endOfTrack = new MetaMessage();
// endOfTrack.setMessage(0x2F, new byte[0], 0);
// outputTrack.add(new MidiEvent(endOfTrack, lastOutputTick + 1));
//
// // 保存
// File outputFile = new File(outputPath);
// outputFile.getParentFile().mkdirs();
// int result = MidiSystem.write(outputSequence, 1, outputFile);
// System.out.println("Written bytes: " + result);
// System.out.println("Saved to: " + outputPath);
//
// } catch (IOException | InvalidMidiDataException e) {
// e.printStackTrace();
// }
// }
// }
//