package bg.petarh.piano.model.key;

public enum KeyMode {
    MAJOR("major"),
    MINOR("minor");

    private final String displayName;

    KeyMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
