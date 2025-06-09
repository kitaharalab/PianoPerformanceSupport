package jp.kthrlab.pianoroll;

import java.awt.*;

public class Channel {
    public int channel_number = 0;
    public int program_number = 0;
    public String program_name = "";
    public Color color = new Color(0, 0, 0, 0);

    public Channel(int channel_number, int program_number, String program_name, Color color) {
        this.channel_number = channel_number;
        this.program_number = program_number;
        this.program_name = program_name;
        this.color = color;
    }
}
