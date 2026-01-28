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

    
    //MIDIノートオンイベントを検出し、PerformanceDataに通知することで演奏の記録や評価を行うモジュール
    PianoTeacherModule(SCCDataSet sccDataSet, PerformanceData performanceData) {
        this.sccDataSet = sccDataSet;
        this.performanceData = performanceData;
    }

    //ノートオンイベント（velocity > 0）を検出したら、PerformanceDataにそのノート番号（msg[1]）を通知する
    @Override
    public void execute(java.lang.Object[] objects, TimeSeriesCompatible[] timeSeriesCompatibles) throws InterruptedException {
        MidiEventWithTicktime midievt = (MidiEventWithTicktime)objects[0];

        byte[] msg = midievt.getMessageInByteArray();
        if ((msg[0] & 0xF0) == 0x90 && msg[2] > 0) {
            performanceData.performed(msg[1]);
        }
        timeSeriesCompatibles[0].add(midievt);
    }

    @Override
    public Class[] getInputClasses() {
        return new java.lang.Class[]{MidiEventWithTicktime.class};
    }

    @Override
    public Class[] getOutputClasses() {
        return new java.lang.Class[] {MidiEventWithTicktime.class};
    }
}
