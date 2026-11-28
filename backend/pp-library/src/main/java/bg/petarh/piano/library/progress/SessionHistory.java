package bg.petarh.piano.library.progress;

import java.util.List;

public interface SessionHistory {

    void record(SessionRecord session);

    List<SessionRecord> history(String key);

    static SessionHistory none() {
        return new SessionHistory() {
            @Override
            public void record(SessionRecord session) {
            }

            @Override
            public List<SessionRecord> history(String key) {
                return List.of();
            }
        };
    }
}
