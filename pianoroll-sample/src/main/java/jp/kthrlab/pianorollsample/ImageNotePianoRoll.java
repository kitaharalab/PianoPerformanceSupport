package jp.kthrlab.pianorollsample;

import jp.crestmuse.cmx.elements.MutableNote;
import jp.crestmuse.cmx.filewrappers.SCC;
import jp.crestmuse.cmx.filewrappers.SCCDataSet;
import jp.kthrlab.pianoroll.cmx.PianoRollDataModelMultiChannel;
import jp.kthrlab.pianoroll.processing_cmx.HorizontalPAppletCmxPianoRoll;
import processing.core.PImage;

import java.awt.*;
import java.util.List;

public class ImageNotePianoRoll extends HorizontalPAppletCmxPianoRoll {
    List<MutableNote> imageNotes = new java.util.ArrayList<>();
    public void addImageNote(MutableNote note) {
        this.imageNotes.add(note);
    }
    public void setImageNotes(List<MutableNote> imageNotes) {
        this.imageNotes = imageNotes;
    }

    @Override
    public void drawNote() {
        if (isNoteVisible && (dataModel != null)) {
            super.drawNote();
            PianoRollDataModelMultiChannel dataModelMultiChannel = (PianoRollDataModelMultiChannel) dataModel;

            long tickPosition = getCmx().getTickPosition();
            Object tickLock = tickPosition;
            synchronized (tickLock) {
                dataModelMultiChannel.getChannels().forEach(channel -> {
                    strokeWeight(1.0f);
                    stroke(channel.color.getRed(), channel.color.getGreen(), channel.color.getBlue(), channel.color.getAlpha());
                    SCCDataSet.Part part = dataModelMultiChannel.getPart(channel.channel_number);

                    if (part != null && part.getNoteOnlyList() != null) {
                        for (MutableNote note : part.getNoteOnlyList()) {

                            Long relativeOnset = note.onset() - tickPosition;
                            float h = (float) ((note.offset() - note.onset()) * dataModelMultiChannel.getPixelPerTick());
                            float y = timeline.getSpan() - (float) (relativeOnset * dataModelMultiChannel.getPixelPerTick()) - h;
                            // Break if it is outside the drawing range
                            if (y < 0) {
                                break;
                            }
                            float x = horizontalKeyboard.semitoneXMap.get(note.notenum());
                            float w = timeline.getSemitoneWidth();

//                            println(String.format("tickPosition=%s, note.onset()=%s, relativeOnset=%s, x=%s, y=%s, w=%s, h=%s",tickPosition, note.onset(), relativeOnset, x, y, w, h));

                            int measure = (int) (note.onset() / dataModelMultiChannel.getScc().getDivision() / dataModelMultiChannel.getBeatNum());
                            double beat = note.onset() / (double) dataModelMultiChannel.getScc().getDivision() - (measure * dataModelMultiChannel.getBeatNum());
                            double duration = (note.offset() - note.onset()) / (double) dataModelMultiChannel.getScc().getDivision();

                            boolean isImageNote = imageNotes.stream().anyMatch(
                                    imageNote -> imageNote.onset() == note.onset() &&
                                            imageNote.offset() == note.offset() &&
                                            imageNote.notenum() == note.notenum()
                            );
                            if (isImageNote) {
                                String path = getClass().getClassLoader().getResource(note.notenum() + ".png").getPath();
                                PImage img = loadImage(path);
                                fill(Color.WHITE.getRGB());
                                noStroke();
                                image(img, x, y, w, h);
                            } else {
                                //ノートによって色を変更
                                fill(channel.color.getRGB());
                                stroke(Color.LIGHT_GRAY.getRGB());
                                this.rect(x, y, w, h);
                            }

                        }
                    }
                    blendMode(MULTIPLY);
                });

            }


        }
        blendMode(1);
    }
}
