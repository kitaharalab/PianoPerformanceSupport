//package jp.kthrlab.pianorollsample;
//
//import java.util.Arrays;
//
//import jp.crestmuse.cmx.amusaj.sp.MidiEventWithTicktime;
//import jp.crestmuse.cmx.amusaj.sp.SPModule;
//import jp.crestmuse.cmx.amusaj.sp.TimeSeriesCompatible;
//import jp.crestmuse.cmx.filewrappers.SCCDataSet;
//import jp.crestmuse.cmx.processing.CMXController;
//
//public class ReceiverModule extends SPModule {
//
//    private static volatile boolean stopRequested = false;
//    CMXController cmx = CMXController.getInstance();
//
//    SCCDataSet sccDataSet;
//
//    public static boolean isStopRequested() {
//        return stopRequested;
//    }
//
//    public static void requestStop() {
//        stopRequested = true;
//    }
//
//    ReceiverModule(SCCDataSet sccDataSet) {
//        this.sccDataSet = sccDataSet;
//    }
//
//    // private void getTargetNotes() {
//    // long tickPosition = cmx.getTickPosition();
//    // java.util.Arrays.stream(sccDataSet.getPart(0).getNoteOnlyList())
//    // .filter(note -> note.onset() == tickPosition)
//    // .collect();
//    // }
//
//    // @Override
//    // public void execute(java.lang.Object[] objects, TimeSeriesCompatible[]
//    // timeSeriesCompatibles) throws InterruptedException {
//    // MidiEventWithTicktime midievt = (MidiEventWithTicktime)objects[0];
//    // byte[] msg = midievt.getMessageInByteArray();
//    // System.out.println("ReceiverModule.execute" + msg[0] + " " + msg[1] + " " +
//    // msg[2]);
//    // }
//
//    @Override
//    public void execute(Object[] objects, TimeSeriesCompatible[] tsc) throws InterruptedException {
//        System.out.println("execute called");
//        MidiEventWithTicktime midievt = (MidiEventWithTicktime) objects[0];
//        byte[] msg = midievt.getMessageInByteArray();
//
//        // MIDI NOTE ON
//        if ((msg[0] & 0xF0) == 0x90 && msg[2] > 0) {
//            int inputPitch = msg[1];
//            long tick = cmx.getTickPosition();
//
//            boolean isCorrect = Arrays.stream(sccDataSet.getPart(0).getNoteOnlyList())
//                    .anyMatch(note -> Math.abs(note.onset() - tick) <= 1 && note.notenum() == inputPitch);
//
//            System.out.println("入力pitch: " + inputPitch + " / tick: " + tick + " → 正解？" + isCorrect);
//
//            if (!isCorrect) {
//                System.out.println("不正ノート → 停止要求");
//                requestStop(); // ← ここでフラグを立てる
//            }
//        }
//    }
//
//    public static void requestStop(boolean value) {
//        stopRequested = value;
//    }
//
//    @Override
//    public Class[] getInputClasses() {
//        return new java.lang.Class[] { MidiEventWithTicktime.class };
//    }
//
//    @Override
//    public Class[] getOutputClasses() {
//        return new java.lang.Class[] { MidiEventWithTicktime.class };
//    }
//}