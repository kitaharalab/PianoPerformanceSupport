package jp.kthrlab.pianoroll;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HorizontalKeyboard extends Keyboard {
    private float keyboardY;
    public List<Float> whiteKeyX;
    public List<Float> blackKeyX;
//    public Map<Integer, Float> keyXMap;
    public Map<Integer, Float> semitoneXMap;

    public HorizontalKeyboard(float width, float height) {
        this(width, height, 60, 21);
    }

    public HorizontalKeyboard(float width, float height, int numKeys, int firstNn) {
        super(width, height, numKeys, firstNn);
        this.keyboardY = height - getWhiteKeyHeight();

        mapCoordinates();
    }

    public float getKeyboardY() {
        return keyboardY;
    }

    private void mapCoordinates(){
        whiteKeyX = new ArrayList<>();
        blackKeyX = new ArrayList<>();
//        keyXMap = new HashMap<>();
        semitoneXMap = new HashMap<>();

        for (int nn = this.getFirstNn(); nn <= this.getLastNn(); nn++) {
            if (Keyboard.isWhiteKey(nn)) {
                float wx = whiteKeyX.size() * getWhiteKeyWidth();
                whiteKeyX.add(wx);
//                keyXMap.put(nn, wx);
            } else {
                float bx = semitoneXMap.size() * getBlackKeyWidth() + getOffset();
                blackKeyX.add(bx);
//                keyXMap.put(nn, bx);
            }
            float semitoneX = semitoneXMap.size() * getBlackKeyWidth() + getOffset();
            semitoneXMap.put(nn, semitoneX);
        }
    }
}