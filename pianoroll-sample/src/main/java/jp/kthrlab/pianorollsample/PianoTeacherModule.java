package jp.kthrlab.pianorollsample;

import jp.crestmuse.cmx.amusaj.sp.MidiEventWithTicktime;
import jp.crestmuse.cmx.amusaj.sp.SPModule;
import jp.crestmuse.cmx.amusaj.sp.TimeSeriesCompatible;
import jp.crestmuse.cmx.filewrappers.SCCDataSet;
import jp.crestmuse.cmx.processing.CMXController;

public class PianoTeacherModule extends SPModule {
    private CMXController cmx = CMXController.getInstance();
    private SCCDataSet sccDataSet;
    private PerformanceData performanceData;
    private RhythmJudge rhythmJudge;
    private MyApp app;
    private boolean finished = false;

    // MIDIノートオンイベントを検出し、PerformanceDataに通知することで演奏の記録や評価を行うモジュール
    PianoTeacherModule(SCCDataSet sccDataSet, PerformanceData performanceData, RhythmJudge rhythmJudge, MyApp app) {
        this.sccDataSet = sccDataSet;
        this.performanceData = performanceData;
        this.rhythmJudge = rhythmJudge;
        this.app = app;
    }

    public void updateSongData(SCCDataSet sccDataSet, PerformanceData performanceData, RhythmJudge rhythmJudge) {
        this.sccDataSet = sccDataSet;
        this.performanceData = performanceData;
        this.rhythmJudge = rhythmJudge;
    }

    // ノートオンイベント（velocity > 0）を検出したら、PerformanceDataにそのノート番号（msg[1]）を通知する
    @Override
    public void execute(java.lang.Object[] objects, TimeSeriesCompatible[] timeSeriesCompatibles)
            throws InterruptedException {
        MidiEventWithTicktime midievt = (MidiEventWithTicktime) objects[0];

        byte[] msg = midievt.getMessageInByteArray();
        
        //★
        if ((msg[0] & 0xF0) == 0x90 && msg[2] > 0) {
            System.out.println("PianoTeacherModule.execute" + msg[0] + " " + msg[1] + " "
                    + msg[2] + " " + cmx.getTickPosition());
            
            // 次に演奏すべき対象ノートを取得し、ピンポイントで削除する
            jp.crestmuse.cmx.elements.MutableNote targetNote = performanceData.getNextNoteToPlay(msg[1]);
            if (targetNote != null) {
                performanceData.performed(targetNote);
            }

            long tick = cmx.getTickPosition();
            String result = rhythmJudge.judge(
                    msg[1],
                    tick);

            if (app.rhythmMode) {
                app.setRhythmResult(result);
            }

            System.out.println(result);
        }

        timeSeriesCompatibles[0].add(midievt);
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
