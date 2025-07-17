package jp.kthrlab.pianorollsample;

import jp.crestmuse.cmx.elements.MutableNote;
import jp.crestmuse.cmx.filewrappers.SCCDataSet;
import jp.crestmuse.cmx.processing.CMXController;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

//2つ以上の正解ノートがある場合は変更する必要がある

public class PerformanceData {
    private LinkedHashMap<Byte, LinkedHashMap<Long, List<MutableNote>>> notesToPlay;
    private SCCDataSet sccDataSet;

    public PerformanceData(SCCDataSet sccDataSet) {
        this.sccDataSet = sccDataSet;
        this.setNotesToPlay(sccDataSet);
    }

    private void setNotesToPlay(SCCDataSet sccDataSet) {
        // パートごと、tickごとにノートを整理してnotesToPlayに格納
        notesToPlay = new LinkedHashMap<>();
        Arrays.stream(sccDataSet.getPartList()).forEach(part -> {
            LinkedHashMap<Long, List<MutableNote>> notesByChannel = new LinkedHashMap<>();

            Arrays.stream(part.getNoteOnlyList()).forEach(note -> {
                List<MutableNote> notesByTick = notesByChannel.get(note.onset());
                if (notesByTick == null) {
                    notesByTick = new ArrayList<>();
                    notesByChannel.put(note.onset(), notesByTick);
                }
                notesByTick.add(note);

            });

            notesToPlay.put(part.channel(), notesByChannel);
        });
    }

    public void setNotesToPlay() {
        this.setNotesToPlay(this.sccDataSet);
    }

    public void performed(byte note) {
        //                .takeWhile(entry -> !result.get())
        // notesToPlayの中から、最初に見つかった「notenumが一致するノート」を削除
        // そのtickやチャンネルにノートがなくなったら、キーごと削除
        for (Map.Entry<Byte, LinkedHashMap<Long, List<MutableNote>>> entry : notesToPlay.entrySet()) {
            Map.Entry<Long, List<MutableNote>> firstEntry = entry.getValue().entrySet().iterator().next();
            for (MutableNote note1 : firstEntry.getValue()) {
                if (note1.notenum() == note) {
                    firstEntry.getValue().remove(note1);
                    break;
                }
            }
            if (firstEntry.getValue().isEmpty()) {
                entry.getValue().remove(firstEntry.getKey());
            }
            if (entry.getValue().isEmpty()) {
                notesToPlay.remove(entry.getKey());
            }
        }
    }

    public boolean hasNotesToPlay(Long tickPosition) {
        // notesToPlayの中で、tickPositionと一致するか、それより前のノートが残っていればtrue
        AtomicBoolean result = new AtomicBoolean(false);
        for (Map.Entry<Byte, LinkedHashMap<Long, List<MutableNote>>> entry : notesToPlay.entrySet()) {
            if (result.get()) {
                break;
            }
            Map.Entry<Long, List<MutableNote>> firstEntry = entry.getValue().entrySet().iterator().next();
            System.out.println("firstEntry.getKey()=" + firstEntry.getKey());
            result.set(firstEntry.getKey().equals(tickPosition) || tickPosition > firstEntry.getKey());
        }
        return result.get();
    }
}
