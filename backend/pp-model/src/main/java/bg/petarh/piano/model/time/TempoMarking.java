package bg.petarh.piano.model.time;

public enum TempoMarking {
    GRAVE("Grave", 40, 20),
    LARGO("Largo", 50, 45),
    ADAGIO("Adagio", 70, 60),
    ANDANTE("Andante", 90, 80),
    MODERATO("Moderato", 110, 100),
    ALLEGRO("Allegro", 130, 120),
    VIVACE("Vivace", 160, 145),
    PRESTO("Presto", 180, 170);

    private final String displayName;
    private final int presetBpm;
    private final int lowestBpm;

    TempoMarking(String displayName, int presetBpm, int lowestBpm) {
        this.displayName = displayName;
        this.presetBpm = presetBpm;
        this.lowestBpm = lowestBpm;
    }

    public static TempoMarking forBpm(int bpm) {
        TempoMarking match = GRAVE;
        for (TempoMarking marking : values()) {
            if (bpm >= marking.lowestBpm) {
                match = marking;
            }
        }
        return match;
    }

    public String displayName() {
        return displayName;
    }

    public Tempo preset() {
        return Tempo.of(presetBpm);
    }
}
