/**
 * One drone of the SkyShow swarm.
 * PROVIDED FILE - DO NOT CHANGE.
 */
public class Drone {

    /** Safety rule for Part C: two drones must never be closer than this. */
    static final int SAFE_DISTANCE_CM = 50;

    final int id;
    final int x;   // centimeters, 0 .. 1,000,000 (the field is 10 km x 10 km)
    final int y;   // centimeters, 0 .. 1,000,000

    Drone(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    private static volatile long distanceChecks = 0;

    /**
     * SQUARED distance between two drones, in cm^2. There is no square root here:
     * if distSq(a,b) < distSq(c,d) then a,b really are the closer pair, so squared
     * values are enough to compare. Keep your delta squared as well.
     *
     * Every call is counted as one distance check, so call it only when you really
     * compare two drones, and store the best value in a variable instead of calling again.
     */
    static long distSq(Drone a, Drone b) {
        distanceChecks++;
        long dx = a.x - b.x;
        long dy = a.y - b.y;
        return dx * dx + dy * dy;
    }

    static long distanceChecks() {
        return distanceChecks;
    }

    static void resetDistanceChecks() {
        distanceChecks = 0;
    }

    @Override
    public String toString() {
        return "#" + id + " (" + x + ", " + y + ")";
    }
}
