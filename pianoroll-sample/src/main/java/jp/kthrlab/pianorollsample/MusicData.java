package jp.kthrlab.pianorollsample;

import jp.crestmuse.cmx.filewrappers.SCC;
import jp.crestmuse.cmx.processing.CMXController;

import javax.xml.transform.TransformerException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

public class MusicData implements IMusicData {

    private final String filename;
    private final int size;
    private final int initial_blank_measures;
    private final int beats_per_measure;
    private final int num_of_measures;
    private final int repeat_times;
    private final int division;

    /**
     * 音楽生成の入出力データ
     *
     * @param filename               伴奏ファイル名
     * @param size                   カーブの座標を保持するリストのサイズ（タイムラインのwidth）
     * @param initial_blank_measures
     * @param beats_per_measure
     * @param num_of_measures
     * @param repeat_times
     * @param division               config.music.division (scc.division は ticksPerBeat)
     */
    public MusicData(String filename, int size, int initial_blank_measures, int beats_per_measure, int num_of_measures, int repeat_times, int division) {
        this.filename = filename;
        this.size = size;
        this.initial_blank_measures = initial_blank_measures;
        this.beats_per_measure = beats_per_measure;
        this.num_of_measures = num_of_measures;
        this.repeat_times = repeat_times;
        this.division = division;

        this.scc = CMXController.readSMFAsSCC(getClass().getClassLoader().getResource(filename).getPath());

        try {
            scc.toDataSet().repeat(
                    (long) (initial_blank_measures * beats_per_measure * scc.getDivision()),
                    (long) ((initial_blank_measures + num_of_measures) * beats_per_measure * scc.getDivision()),
                    repeat_times - 1
            );
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    private SCC scc;

    // multi-channel
    private Set<AbstractMap.SimpleEntry<Integer, List<Integer>>> channelCurveSet = Collections.synchronizedSet(new CopyOnWriteArraySet<>());

//    @Override
//    public void addCurveByChannel(int channel, List<Integer> curve) {
//        channelCurveSet.add(new Pair<>(channel, Collections.synchronizedList(curve)));
//    }
//
//    @Override
//    public void resetMusicData() {
//        // tickPosition must be 0
//        CMXController.getInstance().setTickPosition(0);
//
//        for (Pair<Integer, List<Integer>> entry : channelCurveSet) {
//            List<Integer> curve = entry.getSecond();
//            curve.replaceAll(val -> null);
//
//            SCC.Part channelPart = scc.toDataSet().getFirstPartWithChannel(entry.getFirst());
//            channelPart.remove(channelPart.getNoteList());
//        }
//    }
//
//    @Override
//    public void resetCurves() {
//        for (Pair<Integer, List<Integer>> entry : channelCurveSet) {
//            List<Integer> curve = entry.getSecond();
//            curve.replaceAll(val -> null);
//        }
//    }
//
//    @Override
//    public void resetNotes() {
//        // tickPosition must be 0
//        CMXController.getInstance().setTickPosition(0);
//
//        // remove notes for curves
//        for (Pair<Integer, List<Integer>> entry : channelCurveSet) {
//            SCC.Part channelPart = scc.toDataSet().getFirstPartWithChannel(entry.getFirst());
//            channelPart.remove(channelPart.getNoteList());
//        }
//    }
//
//    @Override
//    public void storeCurveCoordinatesByChannel(int channel, int from, int thru, int y) {
//        for (Pair<Integer, List<Integer>> entry : channelCurveSet) {
//            if (entry.getFirst() == channel) {
//                List<Integer> curve = entry.getSecond();
//                for (int i = from; i <= thru; i++) {
//                    curve.set(i, y);
//                }
//                break;
//            }
//        }
//    }
//
//    @Override
//    public void storeCurveCoordinatesByChannel(int channel, int i, int y) {
//        for (Pair<Integer, List<Integer>> entry : channelCurveSet) {
//            if (entry.getFirst() == channel) {
//                List<Integer> curve = entry.getSecond();
//                curve.set(i, y);
//                break;
//            }
//        }
//    }
//
//    @Override
//    public void storeCurveCoordinates(int from, int thru, int y) {
//        // Do nothing
//    }
//
//    @Override
//    public void storeCurveCoordinates(int i, int y) {
//        // Do nothing
//    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getInitialBlankMeasures() {
        return initial_blank_measures;
    }

    @Override
    public int getBeatsPerMeasure() {
        return beats_per_measure;
    }

    @Override
    public int getNumOfMeasures() {
        return num_of_measures;
    }

    @Override
    public int getRepeatTimes() {
        return repeat_times;
    }

    @Override
    public int getDivision() {
        return division;
    }

    @Override
    public SCC getScc() {
        return scc;
    }

    @Override
    public void setScc(SCC scc) {

    }
}