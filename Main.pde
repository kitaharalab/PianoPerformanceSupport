import jp.crestmuse.cmx.amusaj.sp.MidiInputModule; //<>//
import jp.crestmuse.cmx.amusaj.sp.MidiOutputModule;
import jp.crestmuse.cmx.processing.*;
import jp.crestmuse.cmx.sound.*;
import jp.crestmuse.cmx.filewrappers.*;
import javax.sound.midi.MidiDevice;


CMXController cmx = CMXController.getInstance();
Thread drummerThread;
DataAnalysis da;

int stage = -1;
int cursor = 0;
int mode = 0;
int tempo = 120;
String[] genre = {"Rock", "Jazz"};
int genreID = 0;
double[] alpha = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};   // 0 <= alpha <= 1
int alphaID = 0;

int[][] experimentsData = {{1,0,2}, {0,2,1}, {1,1,0}, {0,0,1}, {1,1,2}, {0,1,1}, {1,2,2}, {1,0,0}, {1,2,0}};  // { {genreID, alphaID, songsID}, ... }

void setup() {
  cmx.showMidiInChooser(this);
  cmx.showMidiOutChooser(this);
  
  size(800, 600);
}

void draw() {
  background(255);
  
  if(stage == -1) {   // titles
    fill(0);
    textSize(50);
    text("DRUMMER", 270, 100);
    textSize(30);
    if(cursor == 0)
      text("→", 250, 250);
    else if(cursor == 1)
      text("→", 250, 300);
    else if(cursor == 2)
      text("→", 250, 350);
    else if(cursor == 3)
      text("→", 250, 400);
    if(mode == 0) {
      text("Mode: session", 300, 250);
      text("Genre: " + genre[genreID], 300, 350);
      text("alpha: " + alpha[alphaID], 300, 400);
    } else
      text("Mode: experiment " + mode, 300, 250);
    text("Tempo: " + tempo, 300, 300);
    text("Press the enter key to start.", 190, 500);
  } else if(stage == 0) {   // count
    fill(0);
    textSize(50);
    text("COUNTS", 280, 100);
    textSize(30);
    text("Press the enter key to count.", 190, 400);
  } else if(stage == 1) {   // playing
    fill(0, 50);
    textSize(50);
    text("PLAYING...", 270, 100);
    textSize(30);
    text("If you want to stop playing, press the leftmost key.", 30, 400);
    drawGraph(keyboardVelAvg, color(255, 0, 0));
    drawGraph(drumsVelAvg, color(0, 0, 255));
  } else {   // finished
    fill(0, 50);
    textSize(50);
    text("FINISHED", 270, 100);
    textSize(30);
    text("Press the enter key to exit.", 190, 400);
    drawGraph(keyboardVelAvg, color(255, 0, 0));
    drawGraph(drumsVelAvg, color(0, 0, 255));
  }
}

void setupModule() {
  try {
    da = new DataAnalysis();
    
    //MidiDevice midiInDevice = SoundUtils.getMidiInDeviceByName("Oxygen 25");
    //MidiDevice midiInDevice = SoundUtils.getMidiInDeviceByName("Digital Piano");
    //MidiDevice midiOutDevice = SoundUtils.getMidiOutDeviceByName("loopMIDI Port A");
    //midiInDevice.open();
    //midiOutDevice.open();
    
    //MidiInputModule mi = new MidiInputModule(midiInDevice);
    //MidiOutputModule mo = new MidiOutputModule(midiOutDevice);
    MidiInputModule mi = cmx.createMidiIn();
    MidiOutputModule mo = cmx.createMidiOut();
    
    DrummerModule drummer = new DrummerModule();
    drummerThread = new Thread(drummer);
  
    cmx.addSPModule(mi);
    cmx.addSPModule(mo);
    cmx.addSPModule(drummer);

    cmx.connect(mi, 0, drummer, 0);
    cmx.connect(drummer, 0, mo, 0);
    
  } catch(Exception e) {
    e.printStackTrace();
  }
}

void drawGraph(ArrayList<Integer> data, color c) {
  for(int i = 0; i < data.size(); i++) {
    if(i != 0) {
      float sx = map(i-1, 0, data.size(), 0, width);
      float sy = map(data.get(i-1), 0, 127, height, 0);
      float ex = map(i, 0, data.size(), 0, width);
      float ey = map(data.get(i), 0, 127, height, 0);
      stroke(c);
      strokeWeight(2);
      line(sx, sy, ex, ey);
    }
  }
}

void keyPressed() {
  if(key == ESC)
    exit();
  
  if(stage == -1) {   // title
    if(key == ENTER) {
      if(1 <= mode && mode <= 9) {
        genreID = experimentsData[mode-1][0];
        alphaID = experimentsData[mode-1][1];
      }
      setupModule();
      stage = 0;
    }
    
    if(key == CODED) {
      if(keyCode == UP) {
        if(0 < cursor)
          cursor--;
      } else if(keyCode == DOWN) {
        if(mode == 0) {
          if(cursor < 3)
            cursor++;
        } else {
          if(cursor < 1)
            cursor++;
        }
      }
      
      if(keyCode == LEFT) {
        if(cursor == 0) {
          if(0 < mode)
            mode--;
        } else if(cursor == 1) {
          if(30 < tempo)
            tempo--;
        } else if(cursor == 2) {
          if(0 < genreID)
            genreID--;
        } else if(cursor == 3) {
          if(0 < alphaID)
            alphaID--;
        }
      } else if(keyCode == RIGHT) {
        if(cursor == 0) {
          if(mode < 9)
            mode++;
        } else if(cursor == 1) {
          if(tempo < 250)
            tempo++;
        } else if(cursor == 2) {
          if(genreID < genre.length-1)
            genreID++;
        } else if(cursor == 3) {
          if(alphaID < alpha.length-1)
            alphaID++;
        }
      }
    }
    
  } else if(stage == 0) {   // counts
    if(key == ENTER) {
      stage = 1;
      cmx.startSP();
      cmx.playMusic();
      drummerThread.start();
      try {
        Thread.sleep(500);
        cmx.setTempoInBPM(tempo);
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
  } else if(stage == 1) {   // playing
  } else {   // finished
    if(key == ENTER)
      exit();
  }
  
}
