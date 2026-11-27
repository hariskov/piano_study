package bg.petarh.piano.generator.notes;

import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.pitch.Pitch;

import java.util.List;
import java.util.Optional;
import java.util.Set;

final class HandPitches {

    private static final int FEWEST_PITCHES = 2;

    private HandPitches() {
    }

    static List<Pitch> forHand(RandomNotesSettings settings, Hand hand) {
        return forHand(settings.playableRange(), settings.key(), settings.hands(), hand);
    }

    static List<Pitch> forHand(KeyboardRange range, KeySignature key, Set<Hand> hands, Hand hand) {
        List<Pitch> pitches = (hands.size() > 1 ? hand.portionOf(range) : Optional.of(range))
                .map(KeyboardRange::pitches).orElse(List.of()).stream()
                .filter(key::contains)
                .toList();
        if (pitches.size() < FEWEST_PITCHES) {
            throw new IllegalArgumentException("Range " + range + " has fewer than "
                    + FEWEST_PITCHES + " notes of " + key + " for the " + hand + " hand");
        }
        return pitches;
    }
}
