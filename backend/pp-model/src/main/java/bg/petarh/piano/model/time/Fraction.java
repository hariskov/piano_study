package bg.petarh.piano.model.time;

public final class Fraction implements Comparable<Fraction> {

    public static final Fraction ZERO = new Fraction(0, 1);

    private final long numerator;
    private final long denominator;

    private Fraction(long numerator, long denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public static Fraction of(long numerator, long denominator) {
        if (denominator == 0) {
            throw new IllegalArgumentException("Denominator must not be zero");
        }
        long sign = Long.signum(denominator);
        long divisor = gcd(Math.abs(numerator), Math.abs(denominator));
        return new Fraction(sign * numerator / divisor, sign * denominator / divisor);
    }

    public static Fraction whole(long value) {
        return new Fraction(value, 1);
    }

    public long numerator() {
        return numerator;
    }

    public long denominator() {
        return denominator;
    }

    public Fraction plus(Fraction other) {
        return of(Math.addExact(Math.multiplyExact(numerator, other.denominator),
                        Math.multiplyExact(other.numerator, denominator)),
                Math.multiplyExact(denominator, other.denominator));
    }

    public Fraction minus(Fraction other) {
        return plus(other.negated());
    }

    public Fraction times(Fraction other) {
        return of(Math.multiplyExact(numerator, other.numerator), Math.multiplyExact(denominator, other.denominator));
    }

    public Fraction times(long factor) {
        return of(Math.multiplyExact(numerator, factor), denominator);
    }

    public Fraction dividedBy(Fraction other) {
        if (other.isZero()) {
            throw new ArithmeticException("Division by zero");
        }
        return of(Math.multiplyExact(numerator, other.denominator), Math.multiplyExact(denominator, other.numerator));
    }

    public boolean isZero() {
        return numerator == 0;
    }

    public boolean isPositive() {
        return numerator > 0;
    }

    @Override
    public int compareTo(Fraction other) {
        return Long.compare(Math.multiplyExact(numerator, other.denominator),
                Math.multiplyExact(other.numerator, denominator));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Fraction fraction
                && fraction.numerator == numerator
                && fraction.denominator == denominator;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(numerator) * 31 + Long.hashCode(denominator);
    }

    @Override
    public String toString() {
        return denominator == 1 ? Long.toString(numerator) : numerator + "/" + denominator;
    }

    private Fraction negated() {
        return new Fraction(Math.negateExact(numerator), denominator);
    }

    private static long gcd(long a, long b) {
        return b == 0 ? Math.max(a, 1) : gcd(b, a % b);
    }
}
