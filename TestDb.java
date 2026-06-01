/**
 * Deprecated local database probe kept only to avoid breaking old references.
 *
 * <p>Use the documented application commands instead:
 * <pre>
 * docker compose up -d postgres
 * mvn spring-boot:run
 * </pre>
 */
public final class TestDb {
    private TestDb() {
    }

    public static void main(String[] args) {
        System.err.println("TestDb is deprecated. Use docker compose and mvn spring-boot:run instead.");
    }
}
