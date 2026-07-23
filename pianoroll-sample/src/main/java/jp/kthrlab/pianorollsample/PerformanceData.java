package jp.kthrlab.pianorollsample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jp.crestmuse.cmx.elements.MutableNote;
import jp.crestmuse.cmx.filewrappers.SCCDataSet;

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

    public void setNotesToPlayForRange(long startTick, long endTick) {
        notesToPlay = new LinkedHashMap<>();
        Arrays.stream(sccDataSet.getPartList()).forEach(part -> {
            LinkedHashMap<Long, List<MutableNote>> notesByChannel = new LinkedHashMap<>();

            Arrays.stream(part.getNoteOnlyList()).forEach(note -> {
                if (note.onset() >= startTick && note.onset() < endTick) {
                    List<MutableNote> notesByTick = notesByChannel.get(note.onset());
                    if (notesByTick == null) {
                        notesByTick = new ArrayList<>();
                        notesByChannel.put(note.onset(), notesByTick);
                    }
                    notesByTick.add(note);
                }
            });

            notesToPlay.put(part.channel(), notesByChannel);
        });
    }

    public boolean hasNotesToPlayForBlock(long startTick, long endTick) {
        for (Map.Entry<Byte, LinkedHashMap<Long, List<MutableNote>>> entry : notesToPlay.entrySet()) {
            for (Map.Entry<Long, List<MutableNote>> tickEntry : entry.getValue().entrySet()) {
                if (tickEntry.getKey() >= startTick && tickEntry.getKey() < endTick && !tickEntry.getValue().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    public long getNextNoteOnsetInBlock(long startTick, long endTick) {
        long minOnset = -1;
        for (Map.Entry<Byte, LinkedHashMap<Long, List<MutableNote>>> entry : notesToPlay.entrySet()) {
            for (Map.Entry<Long, List<MutableNote>> tickEntry : entry.getValue().entrySet()) {
                long onset = tickEntry.getKey();
                if (onset >= startTick && onset < endTick && !tickEntry.getValue().isEmpty()) {
                    if (minOnset == -1 || onset < minOnset) {
                        minOnset = onset;
                    }
                }
            }
        }
        return minOnset;
    }

    public void performed(byte note) {
        long currentTick = jp.crestmuse.cmx.processing.CMXController.getInstance().getTickPosition();
        List<Byte> channelsToRemove = new ArrayList<>();

        for (Map.Entry<Byte, LinkedHashMap<Long, List<MutableNote>>> entry : notesToPlay.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            Map.Entry<Long, List<MutableNote>> firstEntry = entry.getValue().entrySet().iterator().next();

            // 最も古いノートの onset が現在の再生位置 (currentTick) 以下の場合のみ削除対象とする
            if (firstEntry.getKey() <= currentTick) {
                boolean removed = false;
                for (MutableNote note1 : firstEntry.getValue()) {
                    if (note1.notenum() == note) {
                        firstEntry.getValue().remove(note1);
                        removed = true;
                        break;
                    }
                }
                if (removed) {
                    if (firstEntry.getValue().isEmpty()) {
                        entry.getValue().remove(firstEntry.getKey());
                    }
                    if (entry.getValue().isEmpty()) {
                        channelsToRemove.add(entry.getKey());
                    }
                }
            }
        }

        // ループ外で安全に削除
        for (Byte channel : channelsToRemove) {
            notesToPlay.remove(channel);
        }
    }

    public void performed(MutableNote note) {
        if (note == null) return;
        List<Byte> channelsToRemove = new ArrayList<>();

        for (Map.Entry<Byte, LinkedHashMap<Long, List<MutableNote>>> entry : notesToPlay.entrySet()) {
            LinkedHashMap<Long, List<MutableNote>> notesByTick = entry.getValue();
            List<MutableNote> notes = notesByTick.get(note.onset());
            if (notes != null) {
                boolean removed = false;
                for (MutableNote note1 : notes) {
                    if (note1.notenum() == note.notenum() && note1.onset() == note.onset()) {
                        notes.remove(note1);
                        removed = true;
                        break;
                    }
                }
                if (removed) {
                    if (notes.isEmpty()) {
                        notesByTick.remove(note.onset());
                    }
                    if (notesByTick.isEmpty()) {
                        channelsToRemove.add(entry.getKey());
                    }
                }
            }
        }

        for (Byte channel : channelsToRemove) {
            notesToPlay.remove(channel);
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
            result.set(firstEntry.getKey().equals(tickPosition) || tickPosition > firstEntry.getKey());
        }
        return result.get();
    }

    /**
     * 未演奏のノート（notesToPlay）の中で、各チャンネルの最も古いノートのうち、
     * 指定された音高（pitch）と一致するものを返す。
     */
    public MutableNote getNextNoteToPlay(int pitch) {
        MutableNote bestNote = null;
        for (Map.Entry<Byte, LinkedHashMap<Long, List<MutableNote>>> entry : notesToPlay.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            Map.Entry<Long, List<MutableNote>> firstEntry = entry.getValue().entrySet().iterator().next();
            for (MutableNote note : firstEntry.getValue()) {
                if (note.notenum() == pitch) {
                    if (bestNote == null || note.onset() < bestNote.onset()) {
                        bestNote = note;
                    }
                }
            }
        }
        return bestNote;
    }
}

