% Lily was here -- automatically converted by midi2ly.py from C:\Users\songo\PianoPerformanceSupport\pianoroll-sample\src\main\resources\kirakira2_first7_.mid
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
  \skip 1*2 
}

trackAchannelB = \relative c {
  c'4 c g' g 
  | % 2
  a a g2 
  | % 3
  
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
