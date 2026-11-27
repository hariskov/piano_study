package bg.petarh.piano.generator.notes;

import bg.petarh.piano.model.chord.ChordType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RandomNotesOptionsTest {

    @Test
    void defaultsToNoAccidentalsOneNoteValueAndMajorAndMinorChords() {
        RandomNotesOptions options = RandomNotesOptions.defaults();

        assertThat(options.allowAccidentals()).isFalse();
        assertThat(options.mixedNoteValues()).isFalse();
        assertThat(options.chordTypes()).containsExactlyInAnyOrder(ChordType.MAJOR, ChordType.MINOR);
    }

    @Test
    void changesOneOptionAtATime() {
        RandomNotesOptions options = RandomNotesOptions.defaults()
                .withAccidentals(true)
                .withMixedNoteValues(true)
                .withChordTypes(Set.of(ChordType.DOMINANT_SEVENTH));

        assertThat(options).isEqualTo(new RandomNotesOptions(true, true, Set.of(ChordType.DOMINANT_SEVENTH)));
    }

    @Test
    void rejectsEmptyChordTypes() {
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomNotesOptions(false, false, Set.of()));
    }

    @Test
    void keepsItsOwnCopyOfTheChordTypes() {
        Set<ChordType> types = new HashSet<>(Set.of(ChordType.MAJOR));
        RandomNotesOptions options = new RandomNotesOptions(false, false, types);

        types.add(ChordType.MINOR);

        assertThat(options.chordTypes()).containsExactly(ChordType.MAJOR);
    }
}
