package jp.kthrlab.pianoroll;

public class Timeline {
    private Float span; // height or width
    private Float semitoneWidth;

    public Timeline(float span, float semitoneWidth) {
        this.span = span;
        this.semitoneWidth = semitoneWidth;
    }
    
    public Float getSemitoneWidth() {
        return semitoneWidth;
    }

    public Float getSpan() {return span; }

}