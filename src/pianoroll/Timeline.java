package pianoroll;

public class Timeline {
    private float keyWidth;
    public Timeline(float keyWidth) {
        this.keyWidth = keyWidth;
    }
    
    public float getKeyWidth() {
        return keyWidth;
    }
    public void setKeyWidth(float keyWidth) {
        this.keyWidth = keyWidth;
    }
    
}