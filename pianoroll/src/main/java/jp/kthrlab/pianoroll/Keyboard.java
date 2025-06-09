package jp.kthrlab.pianoroll;

public class Keyboard {
    // Constants
    static final float WHITE_KEY_HEIGHT_RATIO = 0.12f; // 鍵盤の高さ比率
    static final float BLACK_KEY_HEIGHT_RATIO = 0.07f; // 鍵盤の高さ比率
    public enum NumOfKeysPerOctave {
        WHITE(7),
        BLACK(5),
        SEMITONES(12);

        private final int value;
        NumOfKeysPerOctave(int value) {this.value = value;}

        public int getValue() {return this.value;}
    }

    private int numKeys = 88;
    private int firstNn = 21;

    private float whiteKeyWidth, whiteKeyHeight, blackKeyWidth, blackKeyHeight, offsetRatio, octaveWidth, offset;


    public Keyboard(float displayWidth, float displayHeight, int numKeys, int firstNn) {
        this.numKeys = numKeys;
        this.firstNn = firstNn;
        this.whiteKeyWidth = displayWidth / getWhiteKeys();
        this.blackKeyWidth = (this.whiteKeyWidth * 7f) / 12f;
        this.whiteKeyHeight = displayHeight * WHITE_KEY_HEIGHT_RATIO;
        this.blackKeyHeight = displayHeight * BLACK_KEY_HEIGHT_RATIO;
        this.offsetRatio = Keyboard.getTimelineOffsetRatio(firstNn);
        this.octaveWidth = this.whiteKeyWidth * 7f;
        this.offset = this.octaveWidth * this.offsetRatio;
    }

    public float getOffset() {
        return offset;
    }
    public float getOctaveWidth() {
        return octaveWidth;
    }
    public float getOffsetRatio() {
        return this.offsetRatio;
    }
    public int getNumKeys() {
        return this.numKeys;
    }
//    public void setNumKeys(int numKeys) {
//        this.numKeys = numKeys;
//    }
    public int getFirstNn() {
        return firstNn;
    }
//    public void setFirstNn(int firstNn) {
//        this.firstNn = firstNn;
//    }
    public int getLastNn() {
        return getLastNoteNumber(numKeys, firstNn);
    }
    public int getWhiteKeys() {
        return countWhiteKeys(numKeys, firstNn);
    }
    public float getWhiteKeyWidth() {
        return whiteKeyWidth;
    }
    public float getWhiteKeyHeight() {
        return whiteKeyHeight;
    }
    public void setWhiteKeyHeight(float keyHeight) {
        this.whiteKeyHeight = keyHeight;
    }
    public void setBlackKeyHeight(float blackKeyHeight) {
        this.blackKeyHeight = blackKeyHeight;
    }
    public float getBlackKeyHeight() {
        return blackKeyHeight;
    }
    public float getBlackKeyWidth() {
        return blackKeyWidth;
    }


    // 鍵盤数と最初のノートナンバーから最後のノートナンバーを算出するメソッド
    static int getLastNoteNumber(int numKeys, int firstNoteNumber) {
        return firstNoteNumber + numKeys - 1;
    }

    // 鍵盤数と最初のノートナンバーから白鍵の数を算出するメソッド
    static int countWhiteKeys(int numKeys, int firstNoteNumber) {
        int count = 0;
        for (int i = 0; i < numKeys; i++) {
            int nn = firstNoteNumber + i;
            int noteInOctave = nn % 12;
            // 白鍵: C, D, E, F, G, A, B → 0, 2, 4, 5, 7, 9, 11
            if (noteInOctave == 0 || noteInOctave == 2 || noteInOctave == 4 ||
                noteInOctave == 5 || noteInOctave == 7 || noteInOctave == 9 || noteInOctave == 11) {
                count++;
            }
        }
        return count;
    }

    public static boolean isWhiteKey(int nn) {
        int noteInOctave = nn % 12;
        return (noteInOctave == 0 || noteInOctave == 2 || noteInOctave == 4 ||
                noteInOctave == 5 || noteInOctave == 7 || noteInOctave == 9 || noteInOctave == 11);
    }

    static float getTimelineOffsetRatio(int firstNn) {
        int noteInOctave = firstNn % 12;
        float offsetRatio = 0.0f;
        switch (noteInOctave) {
            case 0:  // C
                offsetRatio = 0.0f;
                break;
            // case 1:  // C#
            case 2:  // D
                offsetRatio = 1.0f - (1f / 7f) - (10f / 12f);
                break;
            // case 3:  // D#
            case 4:  // E
                offsetRatio = 1.0f - (2f / 7f) - (8f / 12f);
                break;
            case 5:  // F
                offsetRatio = 1.0f - (3f / 7f) - (7f / 12f);
                break;
            // case 6:  // F#
            case 7:  // G
                offsetRatio = 1.0f - (4f / 7f) - (5f / 12f);
                break;
            // case 8:  // G#
            case 9:  // A
                offsetRatio = 1.0f - (5f / 7f) - (3f / 12f);
                break;
            // case 10:  // A#
            case 11: // B
                offsetRatio = 1.0f - (6f / 7f) - (1f / 12f);
                break;
            default:
                offsetRatio = 0.0f;
        }
        return offsetRatio;
    }
    
}
