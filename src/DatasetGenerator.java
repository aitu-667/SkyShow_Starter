import java.util.Random;

/**
 * Builds your personal swarm from your student barcode.
 * The same barcode always gives exactly the same drones.
 * PROVIDED FILE - DO NOT CHANGE. Your barcode goes into Main.java.
 */
public class DatasetGenerator {

    static final int FIELD_CM = 1_000_000;   // the field is 10 km x 10 km

    /** Scenario "Free flight": drones anywhere on the field. The first k drones are always the same for any n >= k. */
    static Drone[] generate(String barcode, int n) {
        Random rand = new Random(seed(barcode, 1));
        Drone[] drones = new Drone[n];
        for (int i = 0; i < n; i++) {
            int x = rand.nextInt(FIELD_CM + 1);
            int y = rand.nextInt(FIELD_CM + 1);
            drones[i] = new Drone(i + 1, x, y);
        }
        return drones;
    }

    /**
     * Scenario "Formation": all drones on one straight line, no two at the same point.
     * vertical = true  -> one column, every drone has the same x.
     * vertical = false -> one row, every drone has the same y.
     */
    static Drone[] generateLine(String barcode, int n, boolean vertical) {
        Random rand = new Random(seed(barcode, vertical ? 2 : 3));
        boolean[] used = new boolean[FIELD_CM + 1];
        Drone[] drones = new Drone[n];
        for (int i = 0; i < n; i++) {
            int v;
            do {
                v = rand.nextInt(FIELD_CM + 1);
            } while (used[v]);
            used[v] = true;
            drones[i] = vertical ? new Drone(i + 1, FIELD_CM / 2, v)
                                 : new Drone(i + 1, v, FIELD_CM / 2);
        }
        return drones;
    }

    /** Scenario "Parade": a regular grid, so a huge number of pairs are at exactly the same distance. */
    static Drone[] generateGrid(int n) {
        int columns = 100;
        int step = FIELD_CM / columns;
        Drone[] drones = new Drone[n];
        for (int i = 0; i < n; i++) {
            drones[i] = new Drone(i + 1, (i % columns) * step, (i / columns) * step);
        }
        return drones;
    }

    /** Checksum of a swarm. The instructor uses it to verify your dataset. */
    static String fingerprint(Drone[] drones) {
        long h = 17;
        for (Drone d : drones) {
            h = h * 31 + d.id * 1_000_003L + d.x * 7_919L + d.y;
        }
        return Long.toHexString(h);
    }

    private static long seed(String barcode, int scenario) {
        if (barcode == null || !barcode.matches("[A-Za-z0-9]+")) {
            throw new IllegalArgumentException(
                "Set BARCODE in Main.java to your own student barcode (letters and digits, no spaces). Now it is: " + barcode);
        }
        return barcode.toUpperCase().hashCode() * 10L + scenario;
    }
}
