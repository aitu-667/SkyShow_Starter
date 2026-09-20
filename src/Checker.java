import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Checks your methods, counts distance checks and prints the results.
 * PROVIDED FILE - DO NOT CHANGE. You do not need to read it.
 */
public class Checker {

    private static final String LINE = "  " + "=".repeat(77);
    private static final int SELF_TEST_SWARMS = 3_000;
    private static final int[] SIZES = {1_000, 2_000, 5_000, 10_000};
    private static final int STRESS_SIZE = 5_000;
    private static final int SHOW_SIZE = 100_000;

    /** A correct Divide & Conquer needs about 2 distance checks per drone. 50 is a very safe ceiling. */
    private static final int CHECKS_PER_DRONE_LIMIT = 50;

    private static final int A = 0, B = 1, C = 2;
    private static final String[] PART_NAME = {"Part A  Brute Force", "Part B  Divide & Conquer", "Part C  Safety limit"};
    private static final String[] status = {"not reached", "not reached", "not reached"};

    private static Function<Drone[], Drone[]> bruteForce;
    private static Function<Drone[], Drone[]> divideAndConquer;

    /** A problem found in the student's code: which part it belongs to, lines that explain it, and whether to print the swarm. */
    private static class Problem extends RuntimeException {
        private static final long serialVersionUID = 1L;
        final int part;
        final String[] details;
        final boolean showSwarm;

        Problem(int part, boolean showSwarm, String... lines) {
            super(lines[0]);
            this.part = part;
            this.details = lines;
            this.showSwarm = showSwarm;
        }
    }

    static void run(String barcode, Function<Drone[], Drone[]> bf, Function<Drone[], Drone[]> dc,
                    ToIntFunction<Drone[]> firstUnsafeN) {
        bruteForce = bf;
        divideAndConquer = dc;

        System.out.println(LINE);
        System.out.println("  SkyShow Collision Alert - Assignment 1");
        System.out.println(LINE);

        Drone[] show;
        try {
            show = DatasetGenerator.generate(barcode, SHOW_SIZE);
        } catch (IllegalArgumentException e) {
            System.out.println();
            System.out.println("  ERROR: " + e.getMessage());
            summary();
            return;
        }
        System.out.println("  Barcode     : " + barcode);
        System.out.println("  Fingerprint : " + DatasetGenerator.fingerprint(Arrays.copyOfRange(show, 0, 10_000)));
        System.out.println();

        if (selfTest() && comparison(show) && stressTests(barcode)) {
            partC(show, firstUnsafeN);
        }
        summary();
    }

    // ------------------------------------------------------------------ steps

    /** Step 1: both algorithms on many small swarms, including ties and drones with the same x. */
    private static boolean selfTest() {
        step("[1/4] Self-test (" + fmt("%,d", SELF_TEST_SWARMS) + " small swarms)");
        Random rand = new Random(42);
        for (int t = 1; t <= SELF_TEST_SWARMS; t++) {
            int n = 2 + rand.nextInt(60);
            int range = (t % 3 == 0) ? 8 : 1_000;
            Drone[] swarm = new Drone[n];
            for (int i = 0; i < n; i++) {
                int x = (t % 4 == 0) ? 7 : rand.nextInt(range);
                swarm[i] = new Drone(i + 1, x, rand.nextInt(range));
            }
            try {
                long bf = pairDist(callOnce(bruteForce, swarm, A));
                status[A] = "PASS";
                long dc = pairDist(callOnce(divideAndConquer, swarm, B));
                if (bf != dc) {
                    throw new Problem(B, true, "The two algorithms give different answers on small swarm #" + t + " (" + n + " drones):",
                            "Brute Force distance^2 = " + bf + ",  Divide & Conquer distance^2 = " + dc);
                }
            } catch (Problem p) {
                fail(p);
                if (p.showSwarm) {
                    final Drone[] s = swarm;
                    if (Arrays.stream(s).allMatch(d -> d.x == s[0].x)) {
                        printDetails("Hint: all drones in this swarm have the same x. Split the array in the middle by index,",
                                     "not by the value of x.");
                    }
                    printSwarm(swarm);
                }
                return false;
            }
        }
        status[A] = "PASS";
        status[B] = "PASS";
        System.out.println("PASS");
        return true;
    }

    /** Step 2: the comparison table for the report. */
    private static boolean comparison(Drone[] show) {
        step("[2/4] Brute Force vs Divide & Conquer");
        String[] header = {"N", "Closest pair", "Distance, cm", "Brute Force checks", "D&C checks", "BF / D&C", "Check"};
        String[][] rows = new String[SIZES.length][];
        try {
            Problem warnA = null, warnB = null;
            for (int r = 0; r < SIZES.length; r++) {
                int n = SIZES[r];
                Drone[] drones = Arrays.copyOfRange(show, 0, n);
                Drone.resetDistanceChecks();
                Drone[] bf = callOnce(bruteForce, drones, A);
                long bfChecks = Drone.distanceChecks();
                Drone.resetDistanceChecks();
                Drone[] dc = callOnce(divideAndConquer, drones, B);
                long dcChecks = Drone.distanceChecks();

                if (pairDist(bf) != pairDist(dc)) {
                    throw new Problem(B, false, "For N = " + fmt("%,d", n) + " the two algorithms give different distances:",
                            "Brute Force " + fmt("%.2f", Math.sqrt(pairDist(bf))) + " cm, Divide & Conquer "
                                    + fmt("%.2f", Math.sqrt(pairDist(dc))) + " cm.");
                }
                long expected = (long) n * (n - 1) / 2;
                if (bfChecks != expected && warnA == null) {
                    warnA = new Problem(A, false,
                            "Brute Force made " + fmt("%,d", bfChecks) + " distance checks for N = " + fmt("%,d", n)
                                    + ", but there are exactly n(n-1)/2 = " + fmt("%,d", expected) + " pairs.",
                            "Check every pair exactly once, and keep the best distance in a variable instead of calling distSq again.");
                }
                if (dcChecks < n / 4 && warnB == null) {
                    warnB = new Problem(B, false,
                            "Divide & Conquer made only " + fmt("%,d", dcChecks) + " distance checks for N = "
                                    + fmt("%,d", n) + ". A correct one needs about " + fmt("%,d", 2L * n) + ".",
                            "Compare two drones with Drone.distSq(a, b) - that is how the work is counted.",
                            "Computing dx * dx + dy * dy by hand does not count and is not accepted.");
                }
                if (dcChecks > (long) CHECKS_PER_DRONE_LIMIT * n && warnB == null) {
                    String[] why = dcChecks >= expected * 0.9 && dcChecks <= expected * 1.1
                            ? new String[] {"That is every pair, so this is Brute Force again, not Divide & Conquer.",
                                            "Write the four parts: BASE CASE, DIVIDE, RECURSIVE CASE, COMBINE."}
                            : new String[] {"A correct one needs about 2. Remember that delta is a SQUARED distance:",
                                            "compare dx*dx < delta and dy*dy < delta, never dx < delta."};
                    warnB = new Problem(B, false,
                            "Divide & Conquer made " + fmt("%,d", dcChecks) + " distance checks for N = " + fmt("%,d", n)
                                    + " - that is " + fmt("%,.0f", (double) dcChecks / n) + " per drone.",
                            why[0], why[1]);
                }
                rows[r] = new String[] {
                    fmt("%,d", n),
                    ids(bf).equals(ids(dc)) ? ids(bf) : ids(bf) + " / " + ids(dc),
                    fmt("%.2f", Math.sqrt(pairDist(bf))),
                    fmt("%,d", bfChecks),
                    fmt("%,d", dcChecks),
                    dcChecks > 0 ? fmt("%,.0f", (double) bfChecks / dcChecks) + "x" : "-",
                    "OK"
                };
            }
            if (warnA != null || warnB != null) {
                if (warnA != null) status[A] = "WARNING";
                if (warnB != null) status[B] = "WARNING";
                System.out.println("WARNING");
                System.out.println();
                printTable(header, rows, new boolean[] {true, false, true, true, true, true, false});
                System.out.println();
                printDetails(warnA != null ? warnA.details : warnB.details);
                if (warnA != null && warnB != null) {
                    printDetails("", "There is also a problem in Divide & Conquer - it will be shown after you fix this one.");
                }
                return false;
            }
        } catch (Problem p) {
            fail(p);
            return false;
        }
        System.out.println("PASS");
        System.out.println();
        printTable(header, rows, new boolean[] {true, false, true, true, true, true, false});
        return true;
    }

    /**
     * Step 3: three formations that break a Divide & Conquer with a missing detail.
     * One column  - every drone is next to the dividing line, so the y-stop must work.
     * One row     - every drone has the same y, so the strip filter must work.
     * Regular grid- huge numbers of equal distances, so delta must be compared as a square.
     */
    private static boolean stressTests(String barcode) {
        System.out.println();
        step("[3/4] Stress test (3 formations, " + fmt("%,d", STRESS_SIZE) + " drones each)");

        String[] names = {"One column", "One row", "Regular grid"};
        Drone[][] sets = {
            DatasetGenerator.generateLine(barcode, STRESS_SIZE, true),
            DatasetGenerator.generateLine(barcode, STRESS_SIZE, false),
            DatasetGenerator.generateGrid(STRESS_SIZE)
        };
        String[][] hints = {
            {"Every drone is next to the dividing line, so the strip holds all of them.",
             "In COMBINE, stop the inner loop as soon as the y-difference reaches delta."},
            {"Every drone has the same y, so the y-stop never helps here.",
             "In COMBINE, put only the drones with |x - midX| < delta into the strip."},
            {"A grid has a huge number of pairs at exactly the same distance.",
             "delta is a SQUARED distance: compare dx*dx < delta and dy*dy < delta, never dx < delta."}
        };
        long limit = (long) CHECKS_PER_DRONE_LIMIT * STRESS_SIZE;
        String[][] rows = new String[sets.length][];
        try {
            int firstBad = -1;
            for (int i = 0; i < sets.length; i++) {
                Drone.resetDistanceChecks();
                Drone[] dc = callOnce(divideAndConquer, sets[i], B);
                long dcChecks = Drone.distanceChecks();
                Drone[] bf = callOnce(bruteForce, sets[i], A);
                if (pairDist(bf) != pairDist(dc)) {
                    throw new Problem(B, false, "Formation \"" + names[i] + "\": the two algorithms give different distances.",
                            "Brute Force " + fmt("%.2f", Math.sqrt(pairDist(bf))) + " cm, Divide & Conquer "
                                    + fmt("%.2f", Math.sqrt(pairDist(dc))) + " cm.");
                }
                boolean ok = dcChecks <= limit;
                if (!ok && firstBad < 0) {
                    firstBad = i;
                }
                rows[i] = new String[] {names[i], fmt("%,d", dcChecks), fmt("%,.1f", (double) dcChecks / STRESS_SIZE),
                                        fmt("%,d", limit), ok ? "OK" : "TOO MANY"};
            }
            if (firstBad >= 0) {
                status[B] = "WARNING";
                System.out.println("WARNING");
                System.out.println();
                printStressTable(rows);
                System.out.println();
                printDetails(hints[firstBad]);
                return false;
            }
        } catch (Problem p) {
            fail(p);
            return false;
        }
        System.out.println("PASS");
        System.out.println();
        printStressTable(rows);
        return true;
    }

    /** Step 4: Part C - the first N at which the show becomes unsafe. */
    private static void partC(Drone[] show, ToIntFunction<Drone[]> firstUnsafeN) {
        System.out.println();
        step("[4/4] Part C: safety limit (" + fmt("%,d", SHOW_SIZE) + " drones)");
        long safeSq = (long) Drone.SAFE_DISTANCE_CM * Drone.SAFE_DISTANCE_CM;
        try {
            Drone.resetDistanceChecks();
            callOnce(divideAndConquer, show, B);
            long oneRun = Drone.distanceChecks();
            long budget = 50 * oneRun;

            String before = DatasetGenerator.fingerprint(show);
            Drone.resetDistanceChecks();
            int n = runWithLimits(firstUnsafeN, show, budget);
            long used = Drone.distanceChecks();

            if (n == STOPPED_TOO_MANY_CHECKS) {
                throw new Problem(C, false,
                        "Stopped after " + fmt("%,d", used) + " distance checks - more than 50 full Divide & Conquer runs",
                        "on all " + fmt("%,d", SHOW_SIZE) + " drones (one run: " + fmt("%,d", oneRun) + " checks).",
                        "Trying N = 2, 3, 4, ... one by one is far too slow.",
                        "Use binary search: if the first N drones are unsafe, then every bigger N is unsafe too.");
            }
            if (n == STOPPED_TOO_SLOW) {
                throw new Problem(C, false, "Part C did not finish in " + PART_C_SECONDS + " seconds.",
                        "Check that every step of your loop makes the search range smaller.");
            }
            if (!before.equals(DatasetGenerator.fingerprint(show))) {
                throw new Problem(C, false, "Part C changed the input array. The drones must stay in order of id - work on copies.");
            }
            if (n < 2 || n > show.length) {
                throw new Problem(C, false, "Part C returned N = " + n + ". It must be between 2 and " + fmt("%,d", show.length) + ".");
            }
            Drone[] pair = callOnce(divideAndConquer, Arrays.copyOfRange(show, 0, n), B);
            boolean unsafeAtN = pairDist(pair) < safeSq;
            boolean safeBefore = n == 2
                    || pairDist(callOnce(divideAndConquer, Arrays.copyOfRange(show, 0, n - 1), B)) >= safeSq;
            if (!unsafeAtN || !safeBefore) {
                throw new Problem(C, false, "Part C returned N = " + fmt("%,d", n) + ", but this is not the first unsafe N:",
                        !unsafeAtN
                                ? "the first " + fmt("%,d", n) + " drones are still safe (no pair closer than " + Drone.SAFE_DISTANCE_CM + " cm)."
                                : "the first " + fmt("%,d", n - 1) + " drones are already unsafe.");
            }
            status[C] = "PASS";
            System.out.println("PASS");
            printDetails("First unsafe N = " + fmt("%,d", n) + ": drones " + ids(pair) + " are "
                            + fmt("%.2f", Math.sqrt(pairDist(pair))) + " cm apart.",
                    "Distance checks used by Part C: " + fmt("%,d", used)
                            + " (one Divide & Conquer run on all " + fmt("%,d", SHOW_SIZE) + " drones: " + fmt("%,d", oneRun) + ").");
        } catch (Problem p) {
            fail(p);
        }
    }

    private static final int STOPPED_TOO_MANY_CHECKS = -1_000_001;
    private static final int STOPPED_TOO_SLOW = -1_000_002;
    private static final int PART_C_SECONDS = 120;

    /**
     * Runs Part C in a background thread and stops waiting if it uses too many distance checks
     * or too much time, so a slow solution gets a clear message instead of a frozen program.
     */
    private static int runWithLimits(ToIntFunction<Drone[]> firstUnsafeN, Drone[] show, long budget) {
        int[] answer = new int[1];
        Throwable[] error = new Throwable[1];
        Thread worker = new Thread(null, () -> {
            try {
                answer[0] = firstUnsafeN.applyAsInt(show);
            } catch (Throwable t) {
                error[0] = t;
            }
        }, "part-c", 256L * 1024 * 1024);
        worker.setDaemon(true);
        worker.start();
        long deadline = System.nanoTime() + PART_C_SECONDS * 1_000_000_000L;
        try {
            while (worker.isAlive()) {
                worker.join(50);
                if (worker.isAlive() && Drone.distanceChecks() > budget) {
                    return STOPPED_TOO_MANY_CHECKS;
                }
                if (worker.isAlive() && System.nanoTime() > deadline) {
                    return STOPPED_TOO_SLOW;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return STOPPED_TOO_SLOW;
        }
        Throwable t = error[0];
        if (t instanceof UnsupportedOperationException) {
            throw new Problem(C, false, t.getMessage() == null ? "Part C is not written yet." : t.getMessage());
        } else if (t instanceof StackOverflowError) {
            throw new Problem(C, false, "Part C crashed: the recursion never stops (StackOverflowError).", where(t));
        } else if (t instanceof Problem) {
            throw (Problem) t;
        } else if (t != null) {
            throw new Problem(C, false, "Part C crashed: " + t.getClass().getSimpleName(),
                    t.getMessage() == null ? "" : t.getMessage(), where(t));
        }
        if (Drone.distanceChecks() > budget) {
            return STOPPED_TOO_MANY_CHECKS;
        }
        return answer[0];
    }

    // ------------------------------------------------------- running student code

    /** Runs an algorithm once and turns every possible mistake into a clear Problem. */
    private static Drone[] callOnce(Function<Drone[], Drone[]> algorithm, Drone[] drones, int part) {
        String name = part == A ? "Brute Force" : "Divide & Conquer";
        String before = DatasetGenerator.fingerprint(drones);
        Drone[] result;
        try {
            result = algorithm.apply(drones);
        } catch (UnsupportedOperationException e) {
            throw new Problem(part, false, e.getMessage() == null ? name + " is not written yet." : e.getMessage());
        } catch (StackOverflowError e) {
            throw new Problem(part, true, name + " crashed: the recursion never stops (StackOverflowError).",
                    where(e), "Check BASE CASE and DIVIDE: split the array in the middle by index.");
        } catch (RuntimeException e) {
            throw new Problem(part, true, name + " crashed: " + e.getClass().getSimpleName(),
                    e.getMessage() == null ? "" : e.getMessage(), where(e));
        }
        if (!before.equals(DatasetGenerator.fingerprint(drones))) {
            throw new Problem(part, false, name + " changed the input array. Work on a copy of it:",
                    "Drone[] copy = drones.clone();");
        }
        Drone[] r = result;
        if (r == null || r.length != 2 || r[0] == null || r[1] == null || r[0] == r[1]
                || !fromInput(r[0], drones) || !fromInput(r[1], drones)) {
            throw new Problem(part, true, name + " must return new Drone[] {a, b}:",
                    "two different drones taken from the input array (not new Drone objects).");
        }
        return r;
    }

    private static boolean fromInput(Drone d, Drone[] drones) {
        return d.id >= 1 && d.id <= drones.length && drones[d.id - 1] == d;
    }

    /** The line in Main.java where the error happened. */
    private static String where(Throwable e) {
        for (StackTraceElement el : e.getStackTrace()) {
            if (el.getClassName().startsWith("Main")) {
                return "Look at Main.java, line " + el.getLineNumber() + ".";
            }
        }
        return "";
    }

    // ------------------------------------------------------------------ output

    private static void step(String label) {
        System.out.print("  " + label + " " + ".".repeat(Math.max(3, 60 - label.length())) + " ");
        System.out.flush();
    }

    private static void fail(Problem p) {
        status[p.part] = "FAILED";
        System.out.println("FAILED");
        printDetails(p.details);
    }

    private static void printDetails(String... lines) {
        for (String line : lines) {
            if (!line.isEmpty()) {
                System.out.println("        " + line);
            }
        }
    }

    private static void printSwarm(Drone[] swarm) {
        System.out.println("        Drones in this swarm (id, x, y):");
        for (int i = 0; i < swarm.length; i += 4) {
            StringBuilder sb = new StringBuilder("          ");
            for (int j = i; j < Math.min(i + 4, swarm.length); j++) {
                sb.append(String.format("%-17s", swarm[j]));
            }
            System.out.println(sb.toString().stripTrailing());
        }
    }

    private static void printStressTable(String[][] rows) {
        String[] header = {"Formation", "D&C checks", "per drone", "Limit", "Check"};
        String[][] filled = new String[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            filled[i] = rows[i] != null ? rows[i] : new String[] {"", "-", "-", "-", "-"};
        }
        printTable(header, filled, new boolean[] {false, true, true, true, false});
    }

    private static void printTable(String[] header, String[][] rows, boolean[] rightAlign) {
        int[] w = new int[header.length];
        for (int c = 0; c < header.length; c++) {
            w[c] = header[c].length();
            for (String[] row : rows) {
                if (row != null) {
                    w[c] = Math.max(w[c], row[c].length());
                }
            }
        }
        StringBuilder border = new StringBuilder("  +");
        for (int width : w) {
            border.append("-".repeat(width + 2)).append('+');
        }
        System.out.println(border);
        System.out.println(tableRow(header, w, rightAlign));
        System.out.println(border);
        for (String[] row : rows) {
            if (row != null) {
                System.out.println(tableRow(row, w, rightAlign));
            }
        }
        System.out.println(border);
    }

    private static String tableRow(String[] cells, int[] w, boolean[] rightAlign) {
        StringBuilder sb = new StringBuilder("  |");
        for (int c = 0; c < cells.length; c++) {
            String pad = " ".repeat(w[c] - cells[c].length());
            sb.append(' ').append(rightAlign[c] ? pad + cells[c] : cells[c] + pad).append(" |");
        }
        return sb.toString();
    }

    /** The last block: one line per part, so you always know what is done and what is not. */
    private static void summary() {
        System.out.println();
        System.out.println(LINE);
        for (int i = 0; i < status.length; i++) {
            System.out.println("  " + PART_NAME[i] + " " + ".".repeat(40 - PART_NAME[i].length()) + " " + status[i]);
        }
        System.out.println(LINE);
        boolean all = true;
        for (String s : status) {
            all &= s.equals("PASS");
        }
        if (all) {
            System.out.println("  RESULT: all checks passed. Copy the tables and the Part C line into your report.");
        } else {
            System.out.println("  RESULT: not finished yet. Read the message above, fix it and run again.");
            System.out.println("          You may still submit: every part marked PASS keeps its points.");
        }
        System.out.println(LINE);
    }

    // ----------------------------------------------------------------- helpers

    /** Squared distance of a pair, computed here without touching the student's distance counter. */
    private static long pairDist(Drone[] pair) {
        long dx = pair[0].x - pair[1].x;
        long dy = pair[0].y - pair[1].y;
        return dx * dx + dy * dy;
    }

    private static String ids(Drone[] pair) {
        return "#" + Math.min(pair[0].id, pair[1].id) + " - #" + Math.max(pair[0].id, pair[1].id);
    }

    /** Always the same number format (1,000 and 12.34), whatever the language of the computer. */
    private static String fmt(String pattern, Object value) {
        return String.format(java.util.Locale.US, pattern, value);
    }
}
