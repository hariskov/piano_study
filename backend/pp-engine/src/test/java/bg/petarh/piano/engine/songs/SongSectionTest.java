package bg.petarh.piano.engine.songs;

import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.score.Score;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SongSectionTest {

    private static final Fraction BAR = Fraction.whole(1);

    private final Score score = Score.builder("Four bars").length(Fraction.whole(4))
            .timeSignature(Fraction.ZERO, TimeSignature.COMMON_TIME)
            .tempo(Fraction.ZERO, Tempo.of(100))
            .tempo(Fraction.of(5, 2), Tempo.of(80))
            .tempo(Fraction.whole(3), Tempo.of(60))
            .build();
    private final SongSelection selection = new SongSelection(
            List.of(note("C4", 0, 1), note("D4", 1, 1), note("E4", 2, 1), note("F4", 3, 1)),
            List.of(note("C3", 1, 2), note("G2", 3, 1)), 0, 0);

    @Test
    void movesTheChosenBarsToTheStart() {
        SectionLoop loop = new SongSection(2, 3).loop(score, selection, 1);

        assertThat(loop.played()).containsExactly(note("D4", 0, 1), note("E4", 1, 1));
        assertThat(loop.length()).isEqualTo(Fraction.whole(2));
        assertThat(loop.passes()).isEqualTo(1);
    }

    @Test
    void cutsNotesThatRunPastTheSectionsEnd() {
        SectionLoop loop = new SongSection(2, 2).loop(score, selection, 1);

        assertThat(loop.accompaniment()).containsExactly(note("C3", 0, 1));
    }

    @Test
    void repeatsTheSectionBackToBack() {
        SectionLoop loop = new SongSection(2, 3).loop(score, selection, 3);

        assertThat(loop.played()).extracting(NoteEvent::start)
                .containsExactly(Fraction.ZERO, BAR, Fraction.whole(2), Fraction.whole(3), Fraction.whole(4),
                        Fraction.whole(5));
        assertThat(loop.length()).isEqualTo(Fraction.whole(6));
        assertThat(loop.passAt(Fraction.of(9, 2))).isEqualTo(3);
        assertThat(loop.passAt(Fraction.ZERO)).isEqualTo(1);
    }

    @Test
    void keepsTheTemposOfTheSectionOnEveryPass() {
        SectionLoop loop = new SongSection(2, 3).loop(score, selection, 2);

        assertThat(loop.tempoMap().tempoAt(Fraction.ZERO).bpm()).isEqualTo(100);
        assertThat(loop.tempoMap().tempoAt(Fraction.of(3, 2)).bpm()).isEqualTo(80);
        assertThat(loop.tempoMap().tempoAt(Fraction.whole(2)).bpm()).isEqualTo(100);
        assertThat(loop.tempoMap().tempoAt(Fraction.of(7, 2)).bpm()).isEqualTo(80);
    }

    @Test
    void theWholeSongIsOnePlainPass() {
        SectionLoop loop = SongSection.whole(score).loop(score, selection, 1);

        assertThat(loop.played()).isEqualTo(selection.played());
        assertThat(loop.tempoMap()).isEqualTo(score.tempoMap());
    }

    @Test
    void refusesBarsOutsideTheSongAndSilentSections() {
        assertThatThrownBy(() -> new SongSection(3, 2)).hasMessageContaining("bar");
        assertThatThrownBy(() -> new SongSection(1, 5).loop(score, selection, 1)).hasMessageContaining("4 bars");
        assertThatThrownBy(() -> new SongSection(1, 1).loop(score, selection, 0)).hasMessageContaining("1 to 99");
        SongSelection leftOnly = new SongSelection(List.of(note("C4", 0, 1)), List.of(), 0, 0);
        assertThatThrownBy(() -> new SongSection(3, 4).loop(score, leftOnly, 1)).hasMessageContaining("no notes");
    }

    private static NoteEvent note(String pitch, int bar, int bars) {
        return new NoteEvent(Pitch.parse(pitch), Fraction.whole(bar), Fraction.whole(bars), Hand.RIGHT);
    }
}
