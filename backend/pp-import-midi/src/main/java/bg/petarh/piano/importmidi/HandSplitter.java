package bg.petarh.piano.importmidi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

final class HandSplitter {

    // LilyPond names each voice's track after its staff: "up:", "upII:", "down:", "downIII:".
    private static final Pattern RIGHT_HAND = Pattern.compile("\\b(right|rh|r\\.h\\.|treble|upper)\\b|^up(?![a-hj-z])");
    private static final Pattern LEFT_HAND = Pattern.compile("\\b(left|lh|l\\.h\\.|bass|lower)\\b|^down(?![a-hj-z])");
    private static final double APART_SEMITONES = 5;

    private enum Hand { RIGHT, LEFT, UNKNOWN }

    private HandSplitter() {
    }

    static List<Draft> arrange(List<Draft> drafts, int splitPoint) {
        List<Draft> pianos = drafts.stream().filter(Draft::piano).toList();
        Optional<Draft> hands = namedHands(pianos).or(() -> twoApart(drafts, pianos));
        List<Draft> arranged = new ArrayList<>();
        for (Draft draft : drafts) {
            if (hands.isPresent() && draft.piano()) {
                if (draft == pianos.getFirst()) {
                    arranged.add(hands.get());
                }
            } else if (draft.piano()) {
                arranged.add(splitAt(draft, splitPoint));
            } else {
                arranged.add(draft);
            }
        }
        return arranged;
    }

    private static Optional<Draft> namedHands(List<Draft> pianos) {
        List<Draft> right = pianos.stream().filter(draft -> hand(draft) == Hand.RIGHT).toList();
        List<Draft> left = pianos.stream().filter(draft -> hand(draft) == Hand.LEFT).toList();
        boolean allNamed = right.size() + left.size() == pianos.size();
        return allNamed && !right.isEmpty() && !left.isEmpty() ? Optional.of(joined(right, left)) : Optional.empty();
    }

    private static Optional<Draft> twoApart(List<Draft> drafts, List<Draft> pianos) {
        if (drafts.size() != 2 || pianos.size() != 2
                || Math.abs(averagePitch(pianos.get(0)) - averagePitch(pianos.get(1))) < APART_SEMITONES) {
            return Optional.empty();
        }
        Draft upper = averagePitch(pianos.get(0)) >= averagePitch(pianos.get(1)) ? pianos.get(0) : pianos.get(1);
        Draft lower = upper == pianos.get(0) ? pianos.get(1) : pianos.get(0);
        return Optional.of(joined(List.of(upper), List.of(lower)));
    }

    private static Hand hand(Draft draft) {
        String name = draft.name().toLowerCase(Locale.ROOT).strip();
        boolean right = RIGHT_HAND.matcher(name).find();
        boolean left = LEFT_HAND.matcher(name).find();
        return right == left ? Hand.UNKNOWN : right ? Hand.RIGHT : Hand.LEFT;
    }

    private static Draft joined(List<Draft> upper, List<Draft> lower) {
        List<Quantiser.Span> spans = new ArrayList<>();
        List<Integer> staves = new ArrayList<>();
        upper.forEach(draft -> draft.spans().forEach(span -> {
            spans.add(span);
            staves.add(1);
        }));
        lower.forEach(draft -> draft.spans().forEach(span -> {
            spans.add(span);
            staves.add(2);
        }));
        return new Draft("Piano", upper.getFirst().instrument(), true, spans, staves);
    }

    private static Draft splitAt(Draft draft, int splitPoint) {
        List<Integer> staves = draft.spans().stream().map(span -> span.pitch() >= splitPoint ? 1 : 2).toList();
        return new Draft(draft.name(), draft.instrument(), true, draft.spans(), staves);
    }

    private static double averagePitch(Draft draft) {
        return draft.spans().stream().mapToInt(Quantiser.Span::pitch).average().orElse(0);
    }
}
