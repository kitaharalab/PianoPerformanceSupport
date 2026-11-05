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

    private long lastInputTick = -1; // 前回のユーザ入力タイミング
    private long lastScoreTick = -1; // 前回の正解ノートタイミング

    private double tolerance = 0.25; // 許容誤差（10%）

    public RhythmJudge(SCCDataSet sccDataSet) {
        //正解データから全ノートを取得
        Arrays.stream(sccDataSet.getPartList()).forEach(part -> {
            scoreNotes.addAll(Arrays.asList(part.getNoteOnlyList()));
        });

        // onset順に並べる
        scoreNotes.sort(Comparator.comparingLong(MutableNote::onset));
    }

    public boolean isFinished() {
        return currentIndex >= scoreNotes.size();
    }

    //リズム判定
    public String judge(byte note, long inputTick) {
        // 終了
        if (currentIndex >= scoreNotes.size()) {
            return "END";
        }

        // 現在の正解ノート
        MutableNote expected = scoreNotes.get(currentIndex);

        // 音が違う
        if (expected.notenum() != note) {
            return "WRONG_NOTE";
        }

        // 1音目基準を作る（ここでは時間差を計算しない）
        if (lastInputTick < 0) {
            lastInputTick = inputTick;
            lastScoreTick = expected.onset();
            currentIndex++;
            return "START";
        }
        
        long inputT = inputTick - lastInputTick;    // ユーザの入力間隔 ユーザの音と音の間のリズムが楽譜と同じかどうかを見ている　オンセット間隔ベース
        long scoreT = expected.onset() - lastScoreTick; // 正解間隔

        // 0で割らないようにする
        if (scoreT == 0) {
            scoreT = 1;
        }

        // タイミング誤差（割合）
        double diff = Math.abs(inputT - scoreT) / (double) scoreT;

        String result;

        if (diff <= tolerance) {
            result = "OK";
        } else if (inputT > scoreT) {
            result = "LATE";
        } else {
            result = "EARLY";
        }

        // 状態更新（次の比較準備）
        lastInputTick = inputTick;
        lastScoreTick = expected.onset();
        currentIndex++;

        return result;
    }

    //初期化
    public void resetForTick(long startTick) {
        currentIndex = 0;
        while (currentIndex < scoreNotes.size() && scoreNotes.get(currentIndex).onset() < startTick) {
            currentIndex++;
        }
        lastInputTick = -1;
        lastScoreTick = -1;
    }

    public void reset() {
        currentIndex = 0;
        lastInputTick = -1;
        lastScoreTick = -1;
    }
}