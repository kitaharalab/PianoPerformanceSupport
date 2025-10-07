package jp.kthrlab.pianorollsample;

import java.awt.Color;
import java.util.List;

import jp.crestmuse.cmx.elements.MutableNote;
import jp.crestmuse.cmx.filewrappers.SCCDataSet;
import jp.kthrlab.pianoroll.cmx.PianoRollDataModelMultiChannel;
import jp.kthrlab.pianoroll.processing_cmx.HorizontalPAppletCmxPianoRoll;
import processing.core.PImage;

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
                    stroke(channel.color.getRed(), channel.color.getGreen(), channel.color.getBlue(),
                            channel.color.getAlpha());
                    SCCDataSet.Part part = dataModelMultiChannel.getPart(channel.channel_number);

                    if (part != null && part.getNoteOnlyList() != null) {
                        for (MutableNote note : part.getNoteOnlyList()) {

                            Long relativeOnset = note.onset() - tickPosition;
                            float h = (float) ((note.offset() - note.onset())
                                    * dataModelMultiChannel.getPixelPerTick());
                            float y = timeline.getSpan()
                                    - (float) (relativeOnset * dataModelMultiChannel.getPixelPerTick()) - h;
                            // Break if it is outside the drawing range
                            if (y < 0) {
                                break;
                            }
                            float x = horizontalKeyboard.semitoneXMap.get(note.notenum());
                            float w = timeline.getSemitoneWidth();

                            // println(String.format("tickPosition=%s, note.onset()=%s, relativeOnset=%s,
                            // x=%s, y=%s, w=%s, h=%s",tickPosition, note.onset(), relativeOnset, x, y, w,
                            // h));

                            int measure = (int) (note.onset() / dataModelMultiChannel.getScc().getDivision()
                                    / dataModelMultiChannel.getBeatNum());
                            double beat = note.onset() / (double) dataModelMultiChannel.getScc().getDivision()
                                    - (measure * dataModelMultiChannel.getBeatNum());
                            double duration = (note.offset() - note.onset())
                                    / (double) dataModelMultiChannel.getScc().getDivision();

                            boolean isImageNote = imageNotes.stream().anyMatch(
                                    imageNote -> imageNote.onset() == note.onset() &&
                                            imageNote.offset() == note.offset() &&
                                            imageNote.notenum() == note.notenum());
                            if (isImageNote) {
                                String path = getClass().getClassLoader().getResource(note.notenum() + ".png")
                                        .getPath();
                                PImage img = loadImage(path);
                                fill(Color.WHITE.getRGB());
                                noStroke();
                                image(img, x, y, w, h);
                            } else {
                                //// 全てのノートを黄色で塗る
                                // fill(channel.color.getRGB());
                                // stroke(Color.LIGHT_GRAY.getRGB());
                                // this.rect(x, y, w, h);

                                //ノートによって色を変更
                                int noteInOctave = note.notenum() % 12;
                                int fillColor;
                            
                                switch (noteInOctave) {
                                case 0: // C
                                fillColor = Color.RED.getRGB();
                                break;
                                case 2: // D
                                fillColor = Color.ORANGE.getRGB();
                                break;
                                case 4: // E
                                fillColor = Color.YELLOW.getRGB();
                                break;
                                case 5: // F
                                fillColor = Color.GREEN.getRGB();
                                break;
                                case 7: // G
                                fillColor = new Color(135, 206, 235).getRGB();// 水色
                                break;
                                case 9: // A
                                fillColor = Color.BLUE.getRGB();
                                break;
                                case 11: // B
                                fillColor = new Color(128, 0, 128).getRGB(); // 紫
                                break;
                                default:
                                fillColor = channel.color.getRGB(); // 黒鍵などは元の色
                                break;
                                }
                            
                                fill(fillColor);
                                stroke(Color.LIGHT_GRAY.getRGB());
                                this.rect(x, y, w, h);

                                //// ノートによって色を変更（オクターブによって彩度を変える）
                                //int noteInOctave = note.notenum() % 12;
                                //int baseColor;
//
                                //switch (noteInOctave) {
                                //    case 0: // C
                                //        baseColor = Color.RED.getRGB();
                                //        break;
                                //    case 2: // D
                                //        baseColor = Color.ORANGE.getRGB();
                                //        break;
                                //    case 4: // E
                                //        baseColor = Color.YELLOW.getRGB();
                                //        break;
                                //    case 5: // F
                                //        baseColor = Color.GREEN.getRGB();
                                //        break;
                                //    case 7: // G
                                //        baseColor = new Color(135, 206, 235).getRGB();// 水色
                                //        break;
                                //    case 9: // A
                                //        baseColor = Color.BLUE.getRGB();
                                //        break;
                                //    case 11: // B
                                //        baseColor = new Color(128, 0, 128).getRGB(); // 紫
                                //        break;
                                //    default:
                                //        baseColor = channel.color.getRGB(); // 黒鍵などは元の色
                                //        break;
                                //}
//
                                //// オクターブ番号を取得
                                //int octave = note.notenum() / 12;
//
                                //// RGB → HSB変換
                                //float[] hsb = Color.RGBtoHSB(
                                //        (baseColor >> 16) & 0xFF,
                                //        (baseColor >> 8) & 0xFF,
                                //        (baseColor) & 0xFF,
                                //        null);
//
                                //// 彩度をオクターブごとに変化させる
                                //// 例: オクターブが高いほど彩度を下げる（0.2〜1.0の範囲）
                                //float saturation = Math.max(0.2f, 1.0f - (octave * 0.1f));
                                //int adjustedColor = Color.HSBtoRGB(hsb[0], saturation, hsb[2]);
//
                                //fill(adjustedColor);
                                //stroke(Color.LIGHT_GRAY.getRGB());
                                //this.rect(x, y, w, h);

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
