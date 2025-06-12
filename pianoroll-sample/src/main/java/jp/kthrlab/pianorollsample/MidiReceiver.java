package jp.kthrlab.pianorollsample;

import javax.sound.midi.*;

public class MidiReceiver implements Receiver {
    private MyApp app;

    public MidiReceiver(MyApp app) {
        this.app = app;
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        if (message instanceof ShortMessage sm) {
            int command = sm.getCommand();
            int pitch = sm.getData1();
            int velocity = sm.getData2();

            if (command == ShortMessage.NOTE_ON && velocity > 0) {
                app.onNoteInput(pitch, velocity);
            } else if (command == ShortMessage.NOTE_OFF || (command == ShortMessage.NOTE_ON && velocity == 0)) {
                app.onNoteOff(pitch);
            }
        }
    }

    @Override
    public void close() {}
}
