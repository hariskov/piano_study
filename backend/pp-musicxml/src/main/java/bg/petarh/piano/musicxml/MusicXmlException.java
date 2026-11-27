package bg.petarh.piano.musicxml;

import java.io.Serial;

public class MusicXmlException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MusicXmlException(String message) {
        super(message);
    }

    public MusicXmlException(String message, Throwable cause) {
        super(message, cause);
    }
}
