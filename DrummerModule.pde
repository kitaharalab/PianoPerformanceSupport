import jp.crestmuse.cmx.amusaj.sp.*; //<>//
import jp.crestmuse.cmx.sound.MIDIConsts;
import jp.crestmuse.cmx.elements.MutableNote;
import jp.crestmuse.cmx.elements.MutableMusicEvent;
import jp.crestmuse.cmx.elements.MutableControlChange;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.InvalidMidiDataException;
import java.util.List;
import java.util.Arrays;
import java.util.Random;

ArrayList<Integer> keyboardVel = new ArrayList<Integer>();
ArrayList<Integer> drumsVel = new ArrayList<Integer>();
ArrayList<Integer> keyboardVelAvg = new ArrayList<Integer>();
ArrayList<Integer> drumsVelAvg = new ArrayList<Integer>();


class DrummerModule extends SPModule implements Runnable {
  String[][] filename = {{"rock01.mid", "rock02.mid", "rock03.mid", "rock04.mid", "rock05.mid"},
                         {"jazz01.mid", "jazz02.mid", "jazz03.mid", "jazz04.mid", "jazz05.mid"}};
  String[][] filenameFillIn = {{"rockFill01.mid", "rockFill02.mid", "rockFill03.mid"},
                               {"jazzFill01.mid", "jazzFill02.mid", "jazzFill03.mid"}};
  String[] expSongs = {"BagsGroove", "CantaloupeIsland", "NowsTheTime", "WatermelonMan"};
  int[] expSongsBars = {145, 133, 145, 133};
  
  List<SCCDataSet> sccs = new ArrayList<SCCDataSet>();
  List<SCCDataSet> sccsFillIn = new ArrayList<SCCDataSet>();
  SCCDataSet currentScc = null;
  SCCDataSet.Part[] parts = null;

  int barCnt = 4;
  int seqStock = 0;
  long barLen;
  int finishCnt = 0;
  int initialDrumVel = 40;
  
  ArrayList<ArrayList<Double>> prmsKey;
  ArrayList<ArrayList<Double>> prmsDrm;

  public DrummerModule() {
    try {
      if(mode == 0) {
        for (int i = 0; i < filename[genreID].length; i++) {
          SCCDataSet scc = CMXController.readSMFAsSCC(sketchPath("midiData/" + filename[genreID][i])).toDataSet();
          sccs.add(scc);
        }
        for(int i = 0; i < filenameFillIn[genreID].length; i++) {
          SCCDataSet scc = CMXController.readSMFAsSCC(sketchPath("midiData/" + filenameFillIn[genreID][i])).toDataSet();
          sccsFillIn.add(scc);
        }
      } else {   // mode: 1~9 (experiment)
        for(int i = 1; i <= expSongsBars[ experimentsData[mode-1][2] ]; i++) {
          SCCDataSet scc = CMXController.readSMFAsSCC(sketchPath("midiData/" + expSongs[ experimentsData[mode-1][2] ] + "/" + expSongs[ experimentsData[mode-1][2] ] + " (" + i + ").mid")).toDataSet();
          sccs.add(scc);
        }
      }

      currentScc = CMXController.readSMFAsSCC(sketchPath("midiData/counts.mid")).toDataSet();
      cmx.smfread(currentScc.getMIDISequence());
      parts = currentScc.getPartsWithChannel(10);

      barLen = 4 * currentScc.getDivision();
      
      prmsKey = da.getParametersKeyboard();
      prmsDrm = da.getParametersDrums();
      
    } 
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  void execute(Object[] src, TimeSeriesCompatible[] dest) throws InterruptedException {
    MidiEventWithTicktime midievt = (MidiEventWithTicktime)src[0]; 
    //dest[0].add(midievt);
    byte[] msg = midievt.getMessageInByteArray();
    if(msg[0] < -8){
      dest[0].add(midievt);
    }
    if(Math.abs(msg[0]) != 112)
      return;
    if(mode == 0 && msg[1] == 21 && finishCnt == 0) {
      finishCnt = 1;
      return;
    }

    println("status: " + msg[0] + " notenum: " + msg[1] + " velocity: " + msg[2]);
    println("midievt.music_position=" + midievt.music_position);
    
    keyboardVel.add((int)msg[2]);

    println();
  }

  void run() {
    while (true) {
      if(finishCnt <= 1) {
      } else {
        if(cmx.isNowPlaying() == false) {
          cmx.stopSP();
          cmx.stopMusic();
          stage = 2;
          break;
        }
      }
      
      if (barCnt*barLen - cmx.getTickPosition() < barLen/2 && finishCnt != 2) {
        seqStock = 0;
      }

      if (seqStock == 0) {
        calcKeyboardVelAvg();
        calcDrumsVelAvg();
        
        if(mode == 0) {
          if(barCnt % 8 == 3) {   // set Fill-in pattern
            SCCDataSet.Part[] newParts = sccsFillIn.get( int(random(filenameFillIn[genreID].length)) ).getPartsWithChannel(10);
            for(int j = 0; j < newParts.length; j++) {
              SCCDataSet.Part newPart = newParts[j];
              for(int k = 0; k < newPart.getNoteOnlyList().length; k++) {
                MutableMusicEvent note = newPart.getNoteOnlyList()[k];
                if(note instanceof MutableNote) {
                  int velocity = velocityChanger(note.notenum(), beatPosition(note.onset()-240));
                  parts[j].addNoteElement(
                    note.onset() + barCnt * barLen,
                    note.offset() + barCnt * barLen,
                    note.notenum(),
                    velocity,
                    note.offVelocity());
                  drumsVel.add(velocity);
                }
              }
            }
            barCnt++;
            if(finishCnt == 1)
              finishCnt = 2;
          } else {                // set normal drum pattern
            SCCDataSet.Part[] newParts = sccs.get( int(random(filename[genreID].length)) ).getPartsWithChannel(10);
            for(int j = 0; j < newParts.length; j++) {
              SCCDataSet.Part newPart = newParts[j];
              for(int k = 0; k < newPart.getNoteOnlyList().length; k++) {
                MutableMusicEvent note = newPart.getNoteOnlyList()[k];
                if(note instanceof MutableNote) {
                  int velocity = velocityChanger(note.notenum(), beatPosition(note.onset()-240));
                  parts[j].addNoteElement(
                    note.onset() + barCnt * barLen, 
                    note.offset() + barCnt * barLen, 
                    note.notenum(), 
                    velocity, 
                    note.offVelocity());
                  drumsVel.add(velocity);
                }
              }
            }
            barCnt++;
          }
          
        } else {   // mode: 1~4 (experiment)
          SCCDataSet.Part[] newParts = sccs.get( barCnt-4 ).getPartsWithChannel(10);
          for(int j = 0; j < newParts.length; j++) {
            SCCDataSet.Part newPart = newParts[j];
            for(int k = 0; k < newPart.getNoteOnlyList().length; k++) {
              MutableMusicEvent note = newPart.getNoteOnlyList()[k];
              if(note instanceof MutableNote) {
                int velocity = velocityChanger(note.notenum(), beatPosition(note.onset()-240));
                parts[j].addNoteElement(
                  note.onset() + barCnt * barLen, 
                  note.offset() + barCnt * barLen, 
                  note.notenum(), 
                  velocity, 
                  note.offVelocity());
                drumsVel.add(velocity);
              }
            }
          }
          barCnt++;
          if(sccs.size()-1 <= barCnt-4)
            finishCnt = 2;
        }
        
        seqStock++;
      }

      try {
        Thread.sleep(60 * 2 * 1000 / tempo);   // half note
      } catch(Exception e) {
        e.printStackTrace();
      }
      
    }
    
  }
  
  void calcKeyboardVelAvg() {
    int sum = 0;
    int cnt = 0;
    for(int i = 0; i < keyboardVel.size(); i++) {
      sum += keyboardVel.get(i);
      cnt++;
    }
    if(cnt != 0)
      keyboardVelAvg.add(sum/cnt);
    else {
      if(keyboardVelAvg.size() >= 1)
        keyboardVelAvg.add( keyboardVelAvg.get(keyboardVelAvg.size()-1) );
      else
        keyboardVelAvg.add(30);
    }
    keyboardVel.clear();
  }
  
  void calcDrumsVelAvg() {
    int sum = 0;
    int cnt = 0;
    for(int i = 0; i < drumsVel.size(); i++) {
      sum += drumsVel.get(i);
      cnt++;
    }
    if(cnt != 0)
      drumsVelAvg.add(sum/cnt);
    else {
      if(drumsVelAvg.size() >= 1)
        drumsVelAvg.add( drumsVelAvg.get(drumsVelAvg.size()-1) );
      else
        drumsVelAvg.add(30);
    }
    drumsVel.clear();
  }
  
  int beatPosition(long onset) {
    int quotient = (int)onset / 480;
    int remainder = (int)onset - (480 * quotient);
    if(remainder == 0)
      return quotient * 2;
    else
      return quotient * 2 + 1;
  }
  
  int velocityChanger(int notenum, int beat) {
    int deviation = 0;
    int mean = initialDrumVel;
    int[] data = {};
    
    switch(notenum) {
      case 36:  // bass drum
        if(genreID == 0) {   // Rock
          if(beat % 2 == 0)
            data = loadDevData("devData/bassRockFront.txt");
          else
            data = loadDevData("devData/bassRockBack.txt");
        } else {             // Jazz
          if(beat % 2 == 0)
            data = loadDevData("devData/bassJazzFront.txt");
          else
            data = loadDevData("devData/bassJazzBack.txt");
        }
        break;
      
      case 38:  // snare drum
        if(genreID == 0) {   // Rock
          if(beat % 2 == 0)
            data = loadDevData("devData/snareRockFront.txt");
          else
            data = loadDevData("devData/snareRockBack.txt");
        } else {             // Jazz
          if(beat % 2 == 0)
            data = loadDevData("devData/snareJazzFront.txt");
          else
            data = loadDevData("devData/snareJazzBack.txt");
        }
        break;
      
      case 42:  // closed hihat
        if(genreID == 0) {   // Rock
          if(beat % 2 == 0)
            data = loadDevData("devData/closedHHRockFront.txt");
          else
            data = loadDevData("devData/closedHHRockBack.txt");
        } else {             // Jazz
          if(beat % 2 == 0)
            data = loadDevData("devData/closedHHJazzFront.txt");
          else
            data = loadDevData("devData/closedHHJazzBack.txt");
        }
        break;
      
      case 44:  // pedal hihat
        break;
      
      case 46:  // hihat open
        if(genreID == 0) {   // Rock
          if(beat % 2 == 0)
            data = loadDevData("devData/HHOpenRockFront.txt");
          else
            data = loadDevData("devData/HHOpenRockBack.txt");
        } else {             // Jazz
          if(beat % 2 == 0)
            data = loadDevData("devData/HHOpenJazzFront.txt");
          else
            data = loadDevData("devData/HHOpenJazzBack.txt");
        }
        break;
      
      case 49:  // crash cymbal
        if(genreID == 0) {   // Rock
          if(beat % 2 == 0)
            data = loadDevData("devData/crashRockFront.txt");
          else
            data = loadDevData("devData/crashRockBack.txt");
        } else {             // Jazz
          if(beat % 2 == 0)
            data = loadDevData("devData/crashJazzFront.txt");
          else
            data = loadDevData("devData/crashJazzBack.txt");
        }
        break;
      
      case 51:  // ride cymbal
        if(genreID == 0) {   // Rock
          if(beat % 2 == 0)
            data = loadDevData("devData/rideRockFront.txt");
          else
            data = loadDevData("devData/rideRockBack.txt");
        } else {             // Jazz
          if(beat % 2 == 0)
            data = loadDevData("devData/rideJazzFront.txt");
          else
            data = loadDevData("devData/rideJazzBack.txt");
        }
        break;
      
      default:
        break;
    }
    
    Random r = new Random();
    deviation = (int)( r.nextGaussian() * standardDeviation(data) + average(data) );
    
    int kSize = keyboardVelAvg.size();
    int dSize = drumsVelAvg.size();
    int nextKeyVel = 0;
    int nextDrmVel = 0;
    if(kSize == 0)
      mean = initialDrumVel;
    else if(kSize == 1) {
      nextKeyVel = (int)( prmsKey.get(0).get(0) + prmsKey.get(0).get(1)*keyboardVelAvg.get(kSize-1) );
      nextDrmVel = (int)( prmsDrm.get(0).get(0) + prmsDrm.get(0).get(1)*drumsVelAvg.get(dSize-1) );
    } else if(kSize == 2) {
      nextKeyVel = (int)( prmsKey.get(1).get(0) + prmsKey.get(1).get(1)*keyboardVelAvg.get(kSize-1) + prmsKey.get(1).get(2)*keyboardVelAvg.get(kSize-2) );
      nextDrmVel = (int)( prmsDrm.get(1).get(0) + prmsDrm.get(1).get(1)*drumsVelAvg.get(dSize-1) + prmsDrm.get(1).get(2)*drumsVelAvg.get(dSize-2) );
    } else {
      nextKeyVel = (int)( prmsKey.get(2).get(0) + prmsKey.get(2).get(1)*keyboardVelAvg.get(kSize-1) + prmsKey.get(2).get(2)*keyboardVelAvg.get(kSize-2) + prmsKey.get(2).get(3)*keyboardVelAvg.get(kSize-3) );
      nextDrmVel = (int)( prmsDrm.get(2).get(0) + prmsDrm.get(2).get(1)*drumsVelAvg.get(dSize-1) + prmsDrm.get(2).get(2)*drumsVelAvg.get(dSize-2) + prmsDrm.get(2).get(3)*drumsVelAvg.get(dSize-3) );
    }
    
    if(kSize != 0)
      mean = (int)( alpha[alphaID] * nextKeyVel + (1-alpha[alphaID]) * nextDrmVel );
    
    int v = deviation + mean;
    if(v >= 0) {
      if(v <= 127)
        return deviation + mean;
      else
        return 127;
    } else
      return 0;
  }
  
  int[] loadDevData(String filename) {
    String[] strData = loadStrings(filename);
    int[] data = new int[strData.length];
    for(int i = 0; i < data.length; i++) {
      data[i] = int(strData[i]);
    }
    return data;
  }
  
  float average(int[] data) {
    int sum = 0;
    int cnt = 0;
    for(int i = 0; i < data.length; i++) {
      sum += data[i];
      cnt++;
    }
    if(cnt != 0)
      return (float)sum/cnt;
    else
      return 0.0;
  }
  
  float standardDeviation(int[] data) {
    float stdDev = 0.0;
    float avg = average(data);
    float sum = 0.0;
    int n = 0;
    for(int i = 0; i < data.length; i++) {
      sum += sq(data[i] - avg);
      n++;
    }
    stdDev = sqrt(sum/n);
    return stdDev;
  }

  Class[] getInputClasses() {
    return new Class[]{MidiEventWithTicktime.class};
  }

  Class[] getOutputClasses() {
    return new Class[]{MidiEventWithTicktime.class};
  }
}
