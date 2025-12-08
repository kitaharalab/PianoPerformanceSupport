% Lily was here -- automatically converted by midi2ly.py from C:\Users\songo\PianoPerformanceSupport\pianoroll-sample\src\main\resources\kirakira2_5_7.mid
\version "2.14.0"

\layout {
  \context {
    \Voice
    \remove Note_heads_engraver
    \consists Completion_heads_engraver
    \remove Rest_engraver
    \consists Completion_rest_engraver
  }
}

trackAchannelA = {
  \skip 1 
  | % 2
  
}

trackAchannelB = \relative c {
  a''4 a g2 
  | % 2
  
}

trackA = <<
  \context Voice = voiceA \trackAchannelA
  \context Voice = voiceB \trackAchannelB
>>


\score {
  <<
    \context Staff=trackA \trackA
  >>
  \layout {}
  \midi {}
}
