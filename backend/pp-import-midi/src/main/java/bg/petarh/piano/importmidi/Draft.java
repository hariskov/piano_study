package bg.petarh.piano.importmidi;

import java.util.List;
import java.util.stream.IntStream;

record Draft(String name, String instrument, boolean piano, List<Quantiser.Span> spans, List<Integer> staves) {

    Draft {
        spans = List.copyOf(spans);
        staves = List.copyOf(staves);
    }

    int staffCount() {
        return staves.isEmpty() ? 1 : 2;
    }

    List<Quantiser.Span> notesOn(int staff) {
        if (staves.isEmpty()) {
            return spans;
        }
        return IntStream.range(0, spans.size())
                .filter(index -> staves.get(index) == staff)
                .mapToObj(spans::get)
                .toList();
    }
}
