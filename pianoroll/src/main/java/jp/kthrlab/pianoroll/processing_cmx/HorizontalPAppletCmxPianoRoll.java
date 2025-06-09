package jp.kthrlab.pianoroll.processing_cmx;

import jp.crestmuse.cmx.filewrappers.SCCDataSet;
import jp.crestmuse.cmx.misc.PianoRoll;
import jp.crestmuse.cmx.processing.CMXController;
import jp.kthrlab.pianoroll.Keyboard;
import jp.kthrlab.pianoroll.cmx.PianoRollDataModelMultiChannel;
import jp.kthrlab.pianoroll.processing.HorizontalPAppletPianoRoll;
import processing.core.PApplet;

import javax.xml.transform.TransformerException;
import java.awt.*;
import java.util.Arrays;

public class HorizontalPAppletCmxPianoRoll extends HorizontalPAppletPianoRoll {

    protected PianoRoll.DataModel dataModel = null;
    private CMXController cmx = CMXController.getInstance();

    @Override
    public void drawNote() {
        if (isNoteVisible && (dataModel != null)) {
            super.drawNote();
            PianoRollDataModelMultiChannel dataModelMultiChannel = (PianoRollDataModelMultiChannel) dataModel;
            dataModelMultiChannel.getChannels().forEach(channel -> {
                strokeWeight(1.0f);
                stroke(channel.color.getRed(), channel.color.getGreen(), channel.color.getBlue(), channel.color.getAlpha());
                SCCDataSet.Part part = dataModelMultiChannel.getPart(channel.channel_number);
                if (part != null && part.getNoteOnlyList() != null) {
                    int pMeasure = 0;
                    Arrays.stream(part.getNoteOnlyList()).forEach(note -> {
//                        if (note.onset() >= (long) (dataModelMultiChannel.getRelativeFirstMeasure() * dataModelMultiChannel.getBeatNum() * dataModelMultiChannel.getScc().getDivision()) &&
//                                note.onset() < (long) (dataModelMultiChannel.getRelativeFirstMeasure() * dataModelMultiChannel.getBeatNum() * dataModelMultiChannel.getScc().getDivision())) {
                            int measure = (int) (note.onset() / dataModelMultiChannel.getScc().getDivision() / dataModelMultiChannel.getBeatNum());



                            double beat = note.onset() / (double) dataModelMultiChannel.getScc().getDivision() - (measure * dataModelMultiChannel.getBeatNum());
                            double duration = (note.offset() - note.onset()) / (double) dataModelMultiChannel.getScc().getDivision();
                            //                            println("measure=$measure, beat=$beat, duration = ${note.offset()}-${note.onset()} / ${scc.division} = $duration")
//                            drawNote(measure - dataModelMultiChannel.getRelativeFirstMeasure(), beat, duration, note.notenum(), false, dataModelMultiChannel);

                            // test draw
                            fill(channel.color.getRGB());
                            stroke(Color.LIGHT_GRAY.getRGB());
//                            float h = (float) ((note.offset() - note.onset()) * dataModelMultiChannel.getPixelPerTick()); //timeline.getPixelPerTick());
                            float h = (float) ((note.offset() - note.onset()) * dataModelMultiChannel.getPixelPerTick()); //timeline.getPixelPerTick());
                            this.rect(
                                    horizontalKeyboard.semitoneXMap.get(note.notenum()),
                                    timeline.getSpan() - (float) (note.onset() * dataModelMultiChannel.getPixelPerTick()) - h,
                                    timeline.getSemitoneWidth(),
                                    h
                            );
//                        }
                    });
                }
                blendMode(MULTIPLY);
            });
        }
        blendMode(1);

//            PianoRollDataModelMultiChannel channelsDataModel = (PianoRollDataModelMultiChannel) dataModel;
//            for (Channel channel : channelsDataModel.getChannels()) {
//                PianoRollDataModelMultiChannel.Color color = channel.getColor();
//                strokeWeight(1.5f);
//                stroke(color.getR(), color.getG(), color.getB(), color.getA());
//        }
    }

    private void drawMeasureLine(){

    }


    //    private void drawNote(int measure, double beat, double duration, int notenum, boolean selected, PianoRollDataModelMultiChannel dataModelMultiChannel) {
//        double lenMeas = (this.height - horizontalKeyboard.getWhiteKeyHeight()) / (double) dataModelMultiChannel.getMeasureNum();
//        double x = horizontalKeyboard.keyXMap.get(notenum); //beat2x(measure, beat);
//        double w = duration * lenMeas / dataModelMultiChannel.getBeatNum();
//        double y = timeline.getSpan() - beat * timeline.getPixelPerTick(); //notenum2y(notenum);
////            fill(color(noteR, noteG, noteB));
////        this.rect(100f, 100f, 100f, 100f);
//        this.rect((float) x, (float) y, (float) w, (float) (timeline.getSemitoneWidth()));// keyboard.getOctaveWidth() / Keyboard.NumOfKeysPerOctave.SEMITONES.getValue()));
//    }

//    protected double beat2x(int measure, double beat) {
//        if (this.dataModel != null) {
//            double lenMeas = (this.musicWidth - this.keyboardWidth) / this.dataModel.getMeasureNum();
//            return this.keyboardWidth + measure * lenMeas + beat * lenMeas / this.dataModel.getBeatNum();
//        }
//        return 0.0;
//    }
//
//    protected Float beat2y() {
//        if (this.dataModel != null) {
//
//        }
//        return 0.0f;
//    }
    public static void main(String[] args) {
        PApplet.main(new String[] { HorizontalPAppletCmxPianoRoll.class.getName() });
    }

}
