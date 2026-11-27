package bg.petarh.piano.generator.notes;

import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.pitch.Pitch;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

final class HandPitches {

    private static final Pitch MIDDLE_C = Pitch.parse("C4");
    private static final int FEWEST_PITCHES = 2;

    private HandPitches() {
    }

    static List<Pitch> forHand(RandomNotesSettings settings, Hand hand) {
        return forHand(settings.playableRange(), settings.key(), settings.hands(), hand);
    }

    static List<Pitch> forHand(KeyboardRange range, KeySignature key, Set<Hand> hands, Hand hand) {
        List<Pitch> pitches = range.pitches().stream()
                .filter(key::contains)
                .filter(hands.size() > 1 ? sideOfMiddleC(hand) : pitch -> true)
                .toList();
        if (pitches.size() < FEWEST_PITCHES) {
            throw new IllegalArgumentException("Range " + range + " has fewer than "
                    + FEWEST_PITCHES + " notes of " + key + " for the " + hand + " hand");
        }
        return pitches;
    }

    private static Predicate<Pitch> sideOfMiddleC(Hand hand) {
        return hand == Hand.RIGHT
                ? pitch -> pitch.compareTo(MIDDLE_C) >= 0
                : pitch -> pitch.compareTo(MIDDLE_C) < 0;
    }
}
