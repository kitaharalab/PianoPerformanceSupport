package jp.kthrlab.pianorollsample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import jp.crestmuse.cmx.elements.MutableNote;
import jp.crestmuse.cmx.filewrappers.SCCDataSet;

public class RhythmJudge {

    private List<MutableNote> scoreNotes = new ArrayList<>();
    private int currentIndex = 0;

    private long lastInputTick = -1;
    private long lastScoreTick = -1;

    private double tolerance = 0.1; // 許容誤差（0.1 = 10%）

    public RhythmJudge(SCCDataSet sccDataSet) {

        Arrays.stream(sccDataSet.getPartList()).forEach(part -> {
            scoreNotes.addAll(Arrays.asList(part.getNoteOnlyList()));
        });

        // onset順に並べる
        scoreNotes.sort(Comparator.comparingLong(MutableNote::onset));
    }

    public boolean isFinished() {
        return currentIndex >= scoreNotes.size();
    }

    /**
     * リズム判定
     */
    public String judge(byte note, long inputTick) {

        if (currentIndex >= scoreNotes.size()) {
            return "END";
        }

        MutableNote expected = scoreNotes.get(currentIndex);

        // 音が違う
        if (expected.notenum() != note) {
            return "WRONG_NOTE";
        }

        // 1音目
        if (lastInputTick < 0) {
            lastInputTick = inputTick;
            lastScoreTick = expected.onset();
            currentIndex++;
            return "START";
        }

        long inputT = inputTick - lastInputTick;
        long scoreT = expected.onset() - lastScoreTick;

        if (scoreT == 0) {
            scoreT = 1;
        }

        double diff = Math.abs(inputT - scoreT) / (double) scoreT;

        String result;

        if (diff <= tolerance) {
            result = "OK";
        } else if (inputT > scoreT) {
            result = "LATE";
        } else {
            result = "EARLY";
        }

        lastInputTick = inputTick;
        lastScoreTick = expected.onset();
        currentIndex++;

        return result;
    }

    /**
     * 初期化
     */
    public void reset() {
        currentIndex = 0;
        lastInputTick = -1;
        lastScoreTick = -1;
    }
}