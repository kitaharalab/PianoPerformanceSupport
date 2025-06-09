package jp.kthrlab.pianoroll;

public interface PianoRoll {

    // キーボードを描画
    void drawKeyboard();

    // タイムラインを描画
    void drawTimeline();

    // ノートを描画
    void drawNote();

    // Playhead（再生位置線）を描画
    void drawPlayhead(); 

} 
