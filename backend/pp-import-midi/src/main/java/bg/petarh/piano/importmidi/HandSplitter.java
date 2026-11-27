package bg.petarh.piano.importmidi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class HandSplitter {

    private static final Pattern RIGHT_HAND = Pattern.compile("\\b(right|rh|r\\.h\\.|treble|upper)\\b");
    private static final Pattern LEFT_HAND = Pattern.compile("\\b(left|lh|l\\.h\\.|bass|lower)\\b");
    private static final double APART_SEMITONES = 5;

    private HandSplitter() {
    }

    static List<Draft> arrange(List<Draft> drafts, int splitPoint) {
        List<Draft> pianos = drafts.stream().filter(Draft::piano).toList();
        boolean namedHands = pianos.size() == 2 && namesHands(pianos.get(0), pianos.get(1));
        boolean onlyTwoApart = drafts.size() == 2 && pianos.size() == 2
                && Math.abs(averagePitch(pianos.get(0)) - averagePitch(pianos.get(1))) >= APART_SEMITONES;
        List<Draft> arranged = new ArrayList<>();
        for (Draft draft : drafts) {
            if ((namedHands || onlyTwoApart) && draft == pianos.get(1)) {
                continue;
            }
            if ((namedHands || onlyTwoApart) && draft == pianos.get(0)) {
                arranged.add(joined(pianos.get(0), pianos.get(1)));
            } else if (draft.piano()) {
                arranged.add(splitAt(draft, splitPoint));
            } else {
                arranged.add(draft);
            }
        }
        return arranged;
    }

    private static boolean namesHands(Draft first, Draft second) {
        String a = first.name().toLowerCase(Locale.ROOT);
        String b = second.name().toLowerCase(Locale.ROOT);
        return (RIGHT_HAND.matcher(a).find() && LEFT_HAND.matcher(b).find())
                || (LEFT_HAND.matcher(a).find() && RIGHT_HAND.matcher(b).find());
    }

    private static Draft joined(Draft first, Draft second) {
        Draft upper = averagePitch(first) >= averagePitch(second) ? first : second;
        Draft lower = upper == first ? second : first;
        List<Quantiser.Span> spans = new ArrayList<>(upper.spans());
        spans.addAll(lower.spans());
        List<Integer> staves = new ArrayList<>();
        upper.spans().forEach(span -> staves.add(1));
        lower.spans().forEach(span -> staves.add(2));
        return new Draft("Piano", true, spans, staves);
    }

    private static Draft splitAt(Draft draft, int splitPoint) {
        List<Integer> staves = draft.spans().stream().map(span -> span.pitch() >= splitPoint ? 1 : 2).toList();
        return new Draft(draft.name(), true, draft.spans(), staves);
    }

    private static double averagePitch(Draft draft) {
        return draft.spans().stream().mapToInt(Quantiser.Span::pitch).average().orElse(0);
    }
}
