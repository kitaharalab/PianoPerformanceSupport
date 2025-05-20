package pianoroll;

public class VerticalTimeline extends Timeline {
    public VerticalTimeline(float keyWidth) {
        super(keyWidth);
    }

    
    // // 縦線のX座標リストを返す
    // public List<Float> getVerticalLinePositions(float width, int numKeys) {
    //     float keyWidth = width / numKeys;
    //     List<Float> positions = new ArrayList<>();
    //     for (int i = 0; i <= numKeys; i++) {
    //         positions.add(i * keyWidth);
    //     }
    //     return positions;
    // }

}
