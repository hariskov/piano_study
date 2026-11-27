package bg.petarh.piano.app.practice;

import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.pitch.Pitch;

import java.util.Collection;
import java.util.stream.Collectors;

public final class TargetNames {

    private TargetNames() {
    }

    public static String describe(Target target) {
        if (target.hands().size() < 2) {
            return names(target.pitches());
        }
        return "R: " + names(target.pitchesFor(Hand.RIGHT)) + "  L: " + names(target.pitchesFor(Hand.LEFT));
    }

    private static String names(Collection<Pitch> pitches) {
        return pitches.stream().map(Pitch::name).collect(Collectors.joining(" "));
    }
}
