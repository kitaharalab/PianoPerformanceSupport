package jp.kthrlab.pianorollsample;

import java.util.Arrays;
import java.util.Optional;

import jp.crestmuse.cmx.amusaj.sp.MidiEventWithTicktime;
import jp.crestmuse.cmx.amusaj.sp.SPModule;
import jp.crestmuse.cmx.amusaj.sp.TimeSeriesCompatible;
import jp.crestmuse.cmx.filewrappers.SCCDataSet;
import jp.crestmuse.cmx.processing.CMXController;

public class ReceiverModule extends SPModule {

    private static volatile boolean stopRequested = false;
    CMXController cmx = CMXController.getInstance();

    SCCDataSet sccDataSet;
    private static long currentTickPosition = 0;
    private long noteLengthMillis = 500;

    public static void setTickPosition(long tick) {
        currentTickPosition = tick;
        // System.out.println("Tick position set to: " + currentTickPosition);
    }

    public static boolean isStopRequested() {
        return stopRequested;
    }

    public static void requestStop() {
        stopRequested = true;
    }

    ReceiverModule(SCCDataSet sccDataSet) {
        this.sccDataSet = sccDataSet;
    }

    // private void getTargetNotes() {
    // long tickPosition = cmx.getTickPosition();
    // java.util.Arrays.stream(sccDataSet.getPart(0).getNoteOnlyList())
    // .filter(note -> note.onset() == tickPosition)
    // .collect();
    // }

    // @Override
    // public void execute(java.lang.Object[] objects, TimeSeriesCompatible[]
    // timeSeriesCompatibles)
    // throws InterruptedException {
    // MidiEventWithTicktime midievt = (MidiEventWithTicktime) objects[0];
    // byte[] msg = midievt.getMessageInByteArray();
    // System.out.println("ReceiverModule.execute" + msg[0] + " " + msg[1] + " " +
    // msg[2]);
    // }

    // executeの中のCMXController cmx =
    // CMXController.getInstance();を使うとmodule内で音楽の停止処理ができる
    // MyAppのdraw()内でtickPositionを取得して、ReceiverModuleのexecute()内でそのtickPositionと一致するノートがあるかどうかを確認する。これは停止判定に使用することができる
    // sccdatasetのgetPartList内にmidiのノートのリストが入っている（どの音か、どのくらいの長さの音なのかなど）onset()でノートの開始時間を取得できる
    // offset()でノートの終了時間を取得できる
    // 正誤判定をするが,draw内で間違っている判定をした場合は、ReceiverModuleのexecute()内でrequestStop()を呼び出して、停止フラグを立てる
    // midiの音を鳴らさないようにして、キーボードから音を鳴らす jamsketchを参考にする

    // バーを隠す挙動のイメージを固める どうカラーバーを隠すのか
    // 画像からでは何小節を隠すかわからないため、tickPositionからどの小節を隠すのかを判定する
    // あっていたらスタートするを実装する。これは、ReceiverModuleのexecute()内で、正しいノートが入力された場合に、CMXControllerのstart()メソッドを呼び出すことで実装できる。

    //public void updateNoteLengthMillis(int inputPitch, long currentTick, int tickTolerance) {
    //    Optional<SCCDataSet.Note> targetNoteOpt = Arrays.stream(sccDataSet.getPart(0).getNoteOnlyList())
    //            .filter(note -> Math.abs(note.onset() - currentTick) <= tickTolerance
    //                    && note.notenum() == inputPitch)
    //            .findFirst();
//
    //    if (targetNoteOpt.isPresent()) {
    //        SCCDataSet.Note note = targetNoteOpt.get();
    //        long noteLengthTick = note.offset() - note.onset();
//
    //        int resolution = sccDataSet.getSequence().getResolution();
    //        float bpm = cmx.getTempoBPM();
    //        double millisPerTick = (60000.0 / bpm) / resolution;
    //        noteLengthMillis = (long) (noteLengthTick * millisPerTick);
    //    }
    //}

    @Override
    public void execute(Object[] objects, TimeSeriesCompatible[] tsc) throws InterruptedException {
        // System.out.println("execute called");
        MidiEventWithTicktime midievt = (MidiEventWithTicktime) objects[0];
        byte[] msg = midievt.getMessageInByteArray();
        long currentTick = currentTickPosition;
        // System.out.println(currentTick);

        // MIDI NOTE ON
        if ((msg[0] & 0xF0) == 0x90 && msg[2] > 0) {
            int inputPitch = msg[1];
            int tickTolerance = 2;

            boolean isCorrect = Arrays.stream(sccDataSet.getPart(0).getNoteOnlyList())
                    .anyMatch(note -> Math.abs(note.onset() - currentTick) <= tickTolerance
                            && note.notenum() == inputPitch);

            // System.out.println("入力pitch: " + inputPitch + " / tick: " + currrentTick + "
            // → 正解？" + isCorrect);

            // if (!isCorrect) {
            // System.out.println("不正ノート → 停止要求");
            // requestStop(); // ← ここでフラグを立てる
            // }
            if (isCorrect) {
                // 正解ノートなら再生再開
                System.out.println("isCorrect");
                cmx.playMusic();
                requestStop(false); // 停止フラグ解除

                //updateNoteLengthMillis(inputPitch, currentTick, tickTolerance);

                //try {
                //    Thread.sleep(noteLengthMillis); // 200ミリ秒だけ再生
                //} catch (InterruptedException e) {
                //    // 無視
                //}
                //cmx.stopMusic();
                //requestStop(true);
            } else {
                // 不正ノートなら停止
                System.out.println("!isCorrect");
                cmx.stopMusic();
                requestStop(true); // 停止フラグセット
            }
        }

        //// MIDI NOTE OFF
        // if ((msg[0] & 0xF0) == 0x80 || ((msg[0] & 0xF0) == 0x90 && msg[2] == 0)) {
        // int inputPitch = msg[1];
        // int tickTolerance = 2;
        //
        // // offset()と一致するノートがあるか判定
        // boolean isNoteOff = Arrays.stream(sccDataSet.getPart(0).getNoteOnlyList())
        // .anyMatch(note -> Math.abs(note.offset() - currentTick) <= tickTolerance
        // && note.notenum() == inputPitch);
        //
        // if (isNoteOff) {
        // System.out.println("ノートオフで停止フラグ解除");
        // requestStop(false); // offsetでのみ停止フラグ解除
        // }
        // }

        // midiout.send(midievt.getMessageInByteArray(), midievt.getTickTime());
        tsc[0].add(midievt);
    }

    public static void requestStop(boolean value) {
        stopRequested = value;
    }

    @Override
    public Class[] getInputClasses() {
        return new java.lang.Class[] { MidiEventWithTicktime.class };
    }

    @Override
    public Class[] getOutputClasses() {
        return new java.lang.Class[] { MidiEventWithTicktime.class };
    }
}