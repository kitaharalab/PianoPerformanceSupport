package jp.kthrlab.pianoroll;

import java.util.HashMap;
import java.util.Map;

public class VerticalTimeline extends Timeline {
    public Map<Long, Float> tickYMap = new HashMap<>();

    public VerticalTimeline(float span, float keyWidth) {
        super(span, keyWidth);
    }

}
