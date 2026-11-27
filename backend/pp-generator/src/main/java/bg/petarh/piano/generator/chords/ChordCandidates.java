package bg.petarh.piano.generator.chords;

import bg.petarh.piano.model.chord.Chord;
import bg.petarh.piano.model.chord.ChordType;
import bg.petarh.piano.model.chord.Inversion;
import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.pitch.Pitch;

import java.util.List;
import java.util.Optional;
import java.util.Set;

final class ChordCandidates {

    private ChordCandidates() {
    }

    static List<Chord> forHand(ChordSettings settings, Hand hand) {
        return forHand(settings.playableRange(), settings.key(), settings.hands(), settings.types(),
                settings.inversions(), hand);
    }

    static List<Chord> forHand(KeyboardRange range, KeySignature key, Set<Hand> hands, Set<ChordType> types,
                               Set<Inversion> inversions, Hand hand) {
        Optional<KeyboardRange> handRange = hands.size() > 1 ? hand.portionOf(range) : Optional.of(range);
        List<Chord> chords = handRange.map(within -> chordsWithin(within, key, types, inversions)).orElse(List.of());
        if (chords.isEmpty()) {
            throw new IllegalArgumentException("No chosen chord fits in " + handRange.orElse(range)
                    + " for the " + hand + " hand");
        }
        return chords;
    }

    private static List<Chord> chordsWithin(KeyboardRange range, KeySignature key, Set<ChordType> types,
                                            Set<Inversion> inversions) {
        return range.pitches().stream()
                .filter(key::contains)
                .flatMap(root -> types.stream().flatMap(type -> inversions.stream()
                        .filter(type::allows)
                        .filter(inversion -> fits(root, type.voicing(inversion), range))
                        .map(inversion -> Chord.of(root, type, inversion))))
                .toList();
    }

    private static boolean fits(Pitch root, List<Integer> voicing, KeyboardRange range) {
        return root.midiNumber() + voicing.getLast() <= range.highest().midiNumber();
    }
}
