package jp.kthrlab.pianorollsample;

import jp.crestmuse.cmx.filewrappers.SCC;

public interface IMusicData {
    String getFilename();

    int getSize();

    int getInitialBlankMeasures();

    int getBeatsPerMeasure();

    int getNumOfMeasures();

    int getRepeatTimes();

    int getDivision();

    SCC getScc();

    void setScc(SCC scc);

//    /**
//     * Reset curves and notes
//     */
//    void resetMusicData();
//
//    void resetCurves();
//
//    void resetNotes();
//
//    void storeCurveCoordinates(int i, int y);
//
//    void storeCurveCoordinates(int from, int thru, int y);
//
//    void storeCurveCoordinatesByChannel(int channel, int i, int y);
//
//    void storeCurveCoordinatesByChannel(int channel, int from, int thru, int y);
//
//    void addCurveByChannel(int channel, java.util.List<Integer> curve);
}