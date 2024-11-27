import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

class DataAnalysis {
  String[] datenames = {"20191129/", "20200109/", "20200116/"};
  String[][] filenames = { {"AutumnLeaves", "JohnnyBGoode", "SomedayMyPrinceWillCome", "TakeTheATrain", "TheChicken"}, 
    {"AllBlues", "BagsGroove", "BlueBossa", "CantaloupeIsland", "FeelLikeMakinLove", "FlyMeToTheMoon", "MilesTones", "MyLittleSuedeShoes", "TheDaysOfWineAndRoses", "WorkSong"}, 
    {"AllBlues", "BagsGroove", "BlueBossa", "CantaloupeIsland", "FeelLikeMakinLove", "FlyMeToTheMoon", "MilesTones", "MyLittleSuedeShoes", "TheDaysOfWineAndRoses", "WorkSong", "I'llRememberApril"} };
  int[][] combi = {{0, 0}, {0, 1}, {0, 2}, {0, 3}, {0, 4}, {1, 0}, {1, 1}, {1, 2}, {1, 3}, {1, 4}, {1, 5}, {1, 6}, {1, 7}, {1, 8}, {1, 9}, {2, 0}, {2, 1}, {2, 2}, {2, 3}, {2, 4}, {2, 5}, {2, 6}, {2, 7}, {2, 8}, {2, 9}, {2, 10}};
  
  int[] all = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25};
  int[] jazz = {0, 2, 3, 5, 6, 7, 10, 11, 12, 13, 14, 15, 16, 17, 20, 21, 22, 23, 24, 25};
  int[] rock = {1, 4, 8, 9, 18, 19};
  int[] highCC = {3, 16, 18, 19};   // more than 0.40
  
  ArrayList<ArrayList<ArrayList<Integer>>> pianoData = new ArrayList<ArrayList<ArrayList<Integer>>>();
    // [ velocity average every 1 bar, notes quantity every 1 bar, max note number every 1 bar ]
  ArrayList<ArrayList<Integer>> drumsData = new ArrayList<ArrayList<Integer>>();
    // velocity average every 1 bar
  
  int targetNN = -1;
    // all drum set : -1
    // ride-cymbal : 51
    // snare-drum : 38
    // closed-hihat : 42
    // hihat-open : 46
    // bass-drum : 36
    // crash-symbal : 49
  
  ArrayList<ArrayList<Double>> parametersKeyboard = new ArrayList<ArrayList<Double>>();
  ArrayList<ArrayList<Double>> parametersDrums = new ArrayList<ArrayList<Double>>();
  
  public DataAnalysis() {
    try {
      for(int i = 0; i < combi.length; i++) {
        SCCXMLWrapper scc = MIDIXMLWrapper.readSMF(sketchPath("performanceData/" + datenames[ combi[i][0] ] + filenames[ combi[i][0] ][ combi[i][1] ] + ".mid")).toSCCXML();
        extractData(scc);
      }
      for (int i = 1; i <= 3; i++) {
        multipleRegressionAnalysis(all, i, 0);
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
    
    //println();
    //for(int i = 0; i < parameters.size(); i++) {
    //  for(int j = 0; j < parameters.get(i).size(); j++) {
    //    println(parameters.get(i).get(j));
    //  }
    //  println();
    //}
  }
  
  void extractData(SCCXMLWrapper scc) {
    ArrayList<Integer> vel01In1bar = new ArrayList<Integer>();
    ArrayList<Integer> vel10In1bar = new ArrayList<Integer>();
    ArrayList<Integer> notesQuantity01In1bar = new ArrayList<Integer>();
    ArrayList<Integer> maxNN01In1bar = new ArrayList<Integer>();
  
    try {
      SCCXMLWrapper.Part[] parts1 = scc.getPartsWithChannel(1);
      SCCXMLWrapper.Part[] parts2 = scc.getPartsWithChannel(2);
      SCCXMLWrapper.Part[] parts10 = scc.getPartsWithChannel(10);
      int velSum01 = 0;
      int notesCnt01 = 0;
      int maxNN01 = 0;
      int velSum10 = 0;
      int notesCnt10 = 0;
      int K01 = 0;
      int L01 = 0;
      int K10 = 0;
      int L10 = 0;
      for (int i = 0; i < parts2.length; i++) {
        SCCXMLWrapper.Note[] notes2 = parts2[i].getNoteOnlyList();
        for (int j = 0; j < notes2.length; j++) {
          if (j != 0 && (notes2[j].notenum()-60) == 0) {
  
            for (int k = K01; k < parts1.length; k++) {
              SCCXMLWrapper.Note[] notes1 = parts1[k].getNoteOnlyList();
              for (int l = L01; l < notes1.length; l++) {
                if (notes1[l].onset() < notes2[j].onset()) {
                  velSum01 += notes1[l].velocity();
                  notesCnt01++;
                  if(notes1[l].notenum() > maxNN01)
                    maxNN01 = notes1[l].notenum();
                } else {
                  if (notesCnt01 != 0)
                    vel01In1bar.add(velSum01/notesCnt01);
                  else
                    vel01In1bar.add(0);
                  notesQuantity01In1bar.add(notesCnt01);
                  maxNN01In1bar.add(maxNN01);
  
                  velSum01 = 0;
                  notesCnt01 = 0;
                  maxNN01 = 0;
                  K01 = k;
                  L01 = l;
                  break;
                }
              }
            }
  
            for (int k = K10; k < parts10.length; k++) {
              SCCXMLWrapper.Note[] notes10 = parts10[k].getNoteOnlyList();
              for (int l = L10; l < notes10.length; l++) {
                if (notes10[l].notenum() != 44) {   // 44: pedal hi-hat
                  if (targetNN == -1 || targetNN == notes10[l].notenum()) {
                    if (notes10[l].onset() < notes2[j].onset()) {
                      velSum10 += notes10[l].velocity();
                      notesCnt10++;
                    } else {
                      if (notesCnt10 != 0)
                        vel10In1bar.add(velSum10/notesCnt10);
                      else
                        vel10In1bar.add(0);
  
                      velSum10 = 0;
                      notesCnt10 = 0;
                      K10 = k;
                      L10 = l;
                      break;
                    }
                  }
                }
              }
            }
          }
        }
      }
      if (notesCnt01 != 0)
        vel01In1bar.add(velSum01/notesCnt01);
      else
        vel01In1bar.add(0);
      notesQuantity01In1bar.add(notesCnt01);
      maxNN01In1bar.add(maxNN01);
      if (notesCnt10 != 0)
        vel10In1bar.add(velSum10/notesCnt10);
      else
        vel10In1bar.add(0);
  
      ArrayList<ArrayList<Integer>> p = new ArrayList<ArrayList<Integer>>();
      p.add(vel01In1bar);
      p.add(notesQuantity01In1bar);
      p.add(maxNN01In1bar);
      pianoData.add(p);
      drumsData.add(vel10In1bar);
    } 
    catch(Exception e) {
      e.printStackTrace();
    }
  }
  
  void multipleRegressionAnalysis(int[] targetData, int expVarNum, int pianodatanum) {
    ArrayList<ArrayList<ArrayList<Integer>>> data = new ArrayList<ArrayList<ArrayList<Integer>>>();   // [ [ [pianoVel, drumsVel], ... ], ... ]
    int sizeSum = 0;
  
    for (int i = 0; i < targetData.length; i++) {
      int size;
      if (pianoData.get( targetData[i] ).get(pianodatanum).size() <= drumsData.get( targetData[i] ).size())
        size = pianoData.get( targetData[i] ).get(pianodatanum).size();
      else
        size = drumsData.get( targetData[i] ).size();
  
      ArrayList<ArrayList<Integer>> d = new ArrayList<ArrayList<Integer>>();   // [ [pianoVel, drumsVel], ... ]
      for (int j = 0; j < size; j++) {
        if (pianoData.get( targetData[i] ).get(pianodatanum).get(j) != 0 && drumsData.get( targetData[i] ).get(j) != 0) {
          ArrayList<Integer> d2 = new ArrayList<Integer>();
          d2.add(pianoData.get( targetData[i] ).get(pianodatanum).get(j));
          d2.add(drumsData.get( targetData[i] ).get(j));
          d.add(d2);
        }
      }
      data.add(d);
      sizeSum += d.size() - expVarNum;
    }
    
    double[] yKey = new double[sizeSum];
    double[][] xKey = new double[sizeSum][expVarNum];
    double[] yDrm = new double[sizeSum];
    double[][] xDrm = new double[sizeSum][expVarNum];
    int S = 0;
  
    for (int i = 0; i < data.size(); i++) {
      for (int j = expVarNum; j < data.get(i).size(); j++) {
        yKey[S] = (double)data.get(i).get(j).get(0);   // piano velocity {Key_n+1}
        yDrm[S] = (double)data.get(i).get(j).get(1);   // drums velocity {Drm_n+1}
        for (int k = 0; k < expVarNum; k++) {
          xKey[S][k] = (double)data.get(i).get(j-k-1).get(0);   // piano velocity {Key_n-k}
          xDrm[S][k] = (double)data.get(i).get(j-k-1).get(1);   // drums velocity {Drm_n-k}
        }
        S++;
      }
    }
    
    parametersKeyboard.add( regressionParameters(yKey, xKey) );   //regression: Kn+1 = a0 + v0*Kn + v1*Kn-1 + ...
    parametersDrums.add( regressionParameters(yDrm, xDrm) );      //regression: Dn+1 = b0 + w0*Dn + w1*Dn-1 + ...
  }
  
  ArrayList<Double> regressionParameters(double[] y, double[][] x) {
    OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
    regression.newSampleData(y, x);
    double[] prm = regression.estimateRegressionParameters();
    ArrayList<Double> p = new ArrayList<Double>();
    for(int i = 0; i < prm.length; i++) {
      p.add(prm[i]);
    }
    //println(regression.calculateAdjustedRSquared());
    return p;
  }
  
  ArrayList<ArrayList<Double>> getParametersKeyboard() {
    return parametersKeyboard;
  }
  
  ArrayList<ArrayList<Double>> getParametersDrums() {
    return parametersDrums;
  }
  
}
