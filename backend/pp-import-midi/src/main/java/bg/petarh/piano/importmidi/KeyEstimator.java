package bg.petarh.piano.importmidi;

import bg.petarh.piano.model.key.KeyMode;
import bg.petarh.piano.model.key.KeySignature;

import java.util.List;

final class KeyEstimator {

    private static final double[] MAJOR = {6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88};
    private static final double[] MINOR = {6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17};
    private static final int PITCH_CLASSES = 12;
    private static final int SEMITONES_PER_FIFTH = 7;
    private static final int RELATIVE_MAJOR_ABOVE_MINOR = 3;
    private static final long DURATION_SCALE = 1024;

    private KeyEstimator() {
    }

    static KeySignature estimate(List<Quantiser.Span> notes) {
        double[] weights = new double[PITCH_CLASSES];
        for (Quantiser.Span note : notes) {
            weights[note.pitch() % PITCH_CLASSES] += note.end().minus(note.start()).times(DURATION_SCALE).floor();
        }
        double best = Double.NEGATIVE_INFINITY;
        KeySignature chosen = KeySignature.major("C");
        for (int tonic = 0; tonic < PITCH_CLASSES; tonic++) {
            double major = correlation(weights, MAJOR, tonic);
            if (major > best) {
                best = major;
                chosen = KeySignature.of(fifthsOfMajor(tonic), KeyMode.MAJOR);
            }
            double minor = correlation(weights, MINOR, tonic);
            if (minor > best) {
                best = minor;
                chosen = KeySignature.of(fifthsOfMajor((tonic + RELATIVE_MAJOR_ABOVE_MINOR) % PITCH_CLASSES), KeyMode.MINOR);
            }
        }
        return chosen;
    }

    private static int fifthsOfMajor(int tonic) {
        int fifths = Math.floorMod(tonic * SEMITONES_PER_FIFTH, PITCH_CLASSES);
        return fifths > 6 ? fifths - PITCH_CLASSES : fifths;
    }

    private static double correlation(double[] weights, double[] profile, int tonic) {
        double meanWeight = mean(weights);
        double meanProfile = mean(profile);
        double covariance = 0;
        double weightSpread = 0;
        double profileSpread = 0;
        for (int pitchClass = 0; pitchClass < PITCH_CLASSES; pitchClass++) {
            double weight = weights[pitchClass] - meanWeight;
            double expected = profile[Math.floorMod(pitchClass - tonic, PITCH_CLASSES)] - meanProfile;
            covariance += weight * expected;
            weightSpread += weight * weight;
            profileSpread += expected * expected;
        }
        double spread = Math.sqrt(weightSpread * profileSpread);
        return spread == 0 ? 0 : covariance / spread;
    }

    private static double mean(double[] values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }
}
