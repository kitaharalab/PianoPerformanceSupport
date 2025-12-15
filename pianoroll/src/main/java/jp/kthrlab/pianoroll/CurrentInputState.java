package jp.kthrlab.pianoroll;

public class CurrentInputState {
    private static volatile int currentNote = -1;

    public static void setCurrentNote(int note) {
        currentNote = note;
    }

    public static int getCurrentNote() {
        return currentNote;
    }
}