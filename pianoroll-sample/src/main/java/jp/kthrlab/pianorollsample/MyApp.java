package jp.kthrlab.pianorollsample;

import jp.crestmuse.cmx.processing.CMXController;
import jp.kthrlab.pianoroll.Channel;
import jp.kthrlab.pianoroll.cmx.PianoRollDataModelMultiChannel;
import jp.kthrlab.pianoroll.processing_cmx.HorizontalPAppletCmxPianoRoll;
import processing.core.PApplet;

import javax.xml.transform.TransformerException;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyApp extends HorizontalPAppletCmxPianoRoll {

    CMXController cmx = CMXController.getInstance();

    IMusicData musicData;

    @Override
    public void setup() {
        super.setup();

        musicData = new MusicData(
                "kirakira2.mid",
                timeline.getSpan().intValue(),
                0,
                4,
                48,
                1,
                12
        );

        List<Color> colors = new ArrayList<>(Arrays.asList(Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN));
        List<Channel> channels = new ArrayList<Channel>();
        try {
            Arrays.stream(musicData.getScc().toDataSet().getPartList()).forEachOrdered(part -> {
                Channel channel = new Channel(
                        part.channel(),
                        part.prognum(),
                        "",
                        colors.get(part.channel())
                );
                channels.add(channel);
            });
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }


        dataModel = new PianoRollDataModelMultiChannel(
                2,
                2 +  12,
                4,
                channels,
                musicData.getScc()
        );

    }


    @Override
    public void draw() {
        super.draw();
        //PDFToImageを呼び出してpdfを表示する
    }


    public static void main(String[] args) {
        PApplet.main(new String[] { MyApp.class.getName() });
    }

}
