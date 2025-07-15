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
    private static long lastCorrectOnset = -1;

    public static long getLastCorrectOnset() {
        return lastCorrectOnset;
    }

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
            int tickTolerance = 0;

            boolean isCorrect = Arrays.stream(sccDataSet.getPart(0).getNoteOnlyList())
                    .anyMatch(note -> Math.abs(note.onset() - currentTick) <= tickTolerance
                            && note.notenum() == inputPitch);

            if (isCorrect) {
                // 正解ノートなら再生再開
                //System.out.println("isCorrect");
                lastCorrectOnset = currentTick;
                requestStop(false); // 停止フラグ解除
                cmx.playMusic();
                System.out.println("ReceicerModule playmusic");
            } else {
                // 不正ノートなら停止
                //System.out.println("!isCorrect");
                requestStop(true); // 停止フラグセット
                cmx.stopMusic();
                System.out.println("ReceicerModule stopmusic");
            }
        }
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