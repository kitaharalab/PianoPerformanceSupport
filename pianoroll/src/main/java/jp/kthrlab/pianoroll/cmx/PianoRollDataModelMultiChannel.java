package jp.kthrlab.pianoroll.cmx;

import jp.crestmuse.cmx.filewrappers.SCC;
import jp.crestmuse.cmx.filewrappers.SCCDataSet.Part;
import jp.crestmuse.cmx.misc.PianoRoll;
import jp.kthrlab.pianoroll.Channel;

import javax.xml.transform.TransformerException;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class PianoRollDataModelMultiChannel implements PianoRoll.DataModel {
    private int relativeFirstMeasure;
    private int relativeLastMeasure;
    private int beatPerMeasure;
    private Double pixelPerTick = 0.1;
    private List<Channel> channels;
    private SCC scc;

    public PianoRollDataModelMultiChannel(int relativeFirstMeasure, int relativeLastMeasure, int beatPerMeasure, List<Channel> channels, SCC scc) {
        this.relativeFirstMeasure = relativeFirstMeasure;
        this.relativeLastMeasure = relativeLastMeasure;
        this.beatPerMeasure = beatPerMeasure;
        this.channels = channels;
        this.scc = scc;
        try {
            this.pixelPerTick = this.pixelPerTick * 120/Integer.parseInt(scc.toDataSet().getFirstTempo().content());
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    public Part getPart(int channel) {
        try {
            return scc.toDataSet().getFirstPartWithChannel(channel);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    public int getRelativeFirstMeasure() {
        return relativeFirstMeasure;
    }

    public int getRelativeLastMeasure() {
        return relativeLastMeasure;
    }

    public int getBeatPerMeasure() {
        return beatPerMeasure;
    }

    public Double getPixelPerTick() {
        return pixelPerTick;
    }

    public void setPixelPerTick(Double pixelPerTick) {
        this.pixelPerTick = pixelPerTick;
    }

    public List<Channel> getChannels() {
        return channels;
    }

    public SCC getScc() {
        return scc;
    }

    @Override
    public int getMeasureNum() {
        return this.relativeLastMeasure - this.relativeFirstMeasure;
    }

    @Override
    public int getBeatNum() {
        return this.beatPerMeasure;
    }

    @Override
    public int getFirstMeasure() {
        return this.relativeFirstMeasure;
    }

    @Override
    public void setFirstMeasure(int measure) {
        this.relativeLastMeasure = measure + this.getMeasureNum();
        this.relativeFirstMeasure = measure;
    }

    @Override
    public boolean isSelectable() {
        return false;
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public void selectNote(int measure, double beat, int notenum) {
        // Do nothing
    }

    @Override
    public boolean isSelected(int measure, double beat, int notenum) {
        return false;
    }

    @Override
    public void drawData(PianoRoll pianoroll) {
        // Do nothing
    }

    @Override
    public int tick2measure(long tick) {
        return (int) (tick / this.scc.getDivision() / this.getBeatNum()) - this.relativeFirstMeasure;
    }

    @Override
    public double tick2beat(long tick) {
        return (double) (tick / this.scc.getDivision() - (tick / this.scc.getDivision() / this.getBeatNum() * this.getBeatNum()));
    }

    @Override
    public void shiftMeasure(int measure) {
        this.relativeFirstMeasure += measure;
        this.relativeLastMeasure += measure;
    }
}