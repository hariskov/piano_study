package bg.petarh.piano.importmidi;

import java.io.Serial;

public class MidiImportException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MidiImportException(String message, Throwable cause) {
        super(message, cause);
    }

    public MidiImportException(String message) {
        super(message);
    }
}
