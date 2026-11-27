package bg.petarh.piano.musicxml;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class ImportReport {

    private final Set<String> warnings = new LinkedHashSet<>();

    void warn(String warning) {
        warnings.add(warning);
    }

    List<String> warnings() {
        return List.copyOf(warnings);
    }
}
