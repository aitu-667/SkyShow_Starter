import java.util.Arrays;
import java.util.Comparator;

/**
 * SkyShow Collision Alert - Assignment 1.
 * This is the only file you change.
 */
public class Main {

    static final String BARCODE = "250825";   // <-- put your student barcode here

    public static void main(String[] args) {
        // Runs your methods, checks them and prints the results.
        Checker.run(BARCODE, Main::findClosestPairBruteForce, Main::findClosestPairDivideAndConquer, Main::findFirstUnsafeN);
    }

    // ======================== Part A: Brute Force ========================

    /** Returns the two closest drones as new Drone[] {a, b}. */
    static Drone[] findClosestPairBruteForce(Drone[] drones) {
        int n = drones.length;
        if (n < 2) return null;
        Drone bestA = null;
        Drone bestB = null;
        long minDistSq = Long.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                long dSq = Drone.distSq(drones[i], drones[j]);
                if (dSq < minDistSq) {
                    minDistSq = dSq;
                    bestA = drones[i];
                    bestB = drones[j];
                }
            }
        }
        return new Drone[] { bestA, bestB };
    }

    // ====================== Part B: Divide & Conquer ======================

    /** Returns the two closest drones as new Drone[] {a, b}. */
    static Drone[] findClosestPairDivideAndConquer(Drone[] drones) {
        Drone[] copy = drones.clone();
        Arrays.sort(copy, Comparator.comparingDouble(d -> d.x));
        return closest(copy, 0, copy.length);
    }

    /**
     * Closest pair among sorted[from] .. sorted[to - 1], where sorted is already sorted by x.
     * Everything below is squared: delta is a squared distance, so compare dx*dx and dy*dy with it.
     */
    private static Drone[] closest(Drone[] sorted, int from, int to) {
        int n = to - from;

        // BASE CASE: 3 drones or fewer - just check all pairs
        if (n <= 3) {
            Drone bestA = sorted[from];
            Drone bestB = sorted[from + 1];
            long minDistSq = Drone.distSq(bestA, bestB);

            for (int i = from; i < to; i++) {
                for (int j = i + 1; j < to; j++) {
                    long dSq = Drone.distSq(sorted[i], sorted[j]);
                    if (dSq < minDistSq) {
                        minDistSq = dSq;
                        bestA = sorted[i];
                        bestB = sorted[j];
                    }
                }
            }
            return new Drone[] { bestA, bestB };
        }

        // DIVIDE: split in the middle BY INDEX; midX is the x of the middle drone
        int mid = from + n / 2;
        double midX = sorted[mid].x;
        // RECURSIVE CASE: solve the left half and the right half, keep the better pair.
        //                 delta = the smaller of the two squared distances.
        Drone[] leftPair = closest(sorted, from, mid);
        Drone[] rightPair = closest(sorted, mid, to);
        long leftDistSq = Drone.distSq(leftPair[0], leftPair[1]);
        long rightDistSq = Drone.distSq(rightPair[0], rightPair[1]);
        long deltaSq = Math.min(leftDistSq, rightDistSq);
        Drone[] bestPair = (leftDistSq < rightDistSq) ? leftPair : rightPair;
        // COMBINE: the closest pair may have one drone on each side.
        // 1) strip = the drones with (x - midX) * (x - midX) < delta
        int stripCount = 0;
        for (int i = from; i < to; i++) {
            double dx = sorted[i].x - midX;
            if (dx * dx < deltaSq) {
                stripCount++;
            }
        }
        Drone[] strip = new Drone[stripCount];
        int index = 0;
        for (int i = from; i < to; i++) {
            double dx = sorted[i].x - midX;
            if (dx * dx < deltaSq) {
                strip[index++] = sorted[i];
            }
        }
        //          2) sort the strip by y
        Arrays.sort(strip, Comparator.comparingDouble(d -> d.y));
        //          3) for each drone in the strip, compare it with the next ones and
        //             stop as soon as (y difference) * (y difference) >= delta
        for (int i = 0; i < strip.length; i++) {
            for (int j = i + 1; j < strip.length; j++) {
                double dy = strip[j].y - strip[i].y;
                if (dy * dy >= deltaSq) {
                    break; // dy*dy >= deltaSq болса тоқтаймыз
                }
                long dSq = Drone.distSq(strip[i], strip[j]);
                if (dSq < deltaSq) {
                    deltaSq = dSq;
                    bestPair = new Drone[] { strip[i], strip[j] };
                }
            }
        }
        return bestPair;
    }

    // ======================== Part C: Safety limit ========================

    /**
     * Drones join the show one by one in order of id: #1, #2, #3, ...
     * Returns the smallest N such that the first N drones already have a pair
     * closer than Drone.SAFE_DISTANCE_CM.
     */
    static int findFirstUnsafeN(Drone[] drones) {
        int low = 2;
        int high = drones.length;
        int ans = high;
        long safeDistSq = (long) Drone.SAFE_DISTANCE_CM * Drone.SAFE_DISTANCE_CM;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            Drone[] sub = Arrays.copyOfRange(drones, 0, mid);
            Drone[] pair = findClosestPairDivideAndConquer(sub);
            long distSq = Drone.distSq(pair[0], pair[1]);
            if (distSq < safeDistSq) {
                ans = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return ans;
    }
}
