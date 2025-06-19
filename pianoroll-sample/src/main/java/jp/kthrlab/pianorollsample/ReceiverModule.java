//package jp.kthrlab.pianorollsample;
//
//import jp.crestmuse.cmx.amusaj.sp.*;
//import jp.crestmuse.cmx.inference.NoteInputListener;
//import jp.crestmuse.cmx.sound.MIDIConsts;
//
//import javax.sound.midi.ShortMessage;
//import java.util.Arrays;
//
//public class ReceiverModule extends SPModule {
//
//    private final NoteInputListener listener;
//
//    public ReceiverModule(NoteInputListener listener) {
//        this.listener = listener;
//    }
//
//    @Override
//    public void execute(Object[] src, Time[] timestamps) {
//        MIDIEventWithTicktime ev = (MIDIEventWithTicktime) src[0];
//        int status = ev.getStatus();
//        int command = status & 0xF0;
//
//        int pitch = ev.getData1();
//        int velocity = ev.getData2();
//
//        switch (command) {
//            case 0x90: // NOTE ON
//                if (velocity > 0) {
//                    listener.onNoteInput(pitch, velocity);
//                } else {
//                    listener.onNoteOff(pitch); // NOTE ON with 0 velocity = NOTE OFF
//                }
//                break;
//            case 0x80: // NOTE OFF
//                listener.onNoteOff(pitch);
//                break;
//            default:
//                break;
//        }
//
//        // 出力も必要ならここに addOutput(0, ev); を入れる
//        addOutput(0, ev); // 忘れずに出力へ渡す
//    }
//
//    @Override
//    public int getInputCount() {
//        return 1;
//    }
//
//    @Override
//    public Class<?> getInputClass(int index) {
//        return MIDIEventWithTicktime.class;
//    }
//
//    @Override
//    public int getOutputCount() {
//        return 1;
//    }
//
//    @Override
//    public Class<?> getOutputClass(int index) {
//        return MIDIEventWithTicktime.class;
//    }
//
//    @Override
//    public Class<?>[] getOutputClasses() {
//        return new Class<?>[] { MIDIEventWithTicktime.class };
//    }
//
//    @Override
//    public String getInputName(int index) {
//        return "MIDI Input";
//    }
//
//    @Override
//    public String getOutputName(int index) {
//        return "MIDI Output";
//    }
//}
