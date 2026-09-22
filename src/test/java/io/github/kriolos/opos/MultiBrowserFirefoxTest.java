package io.github.kriolos.opos;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;

/**
 * Multi-browser concurrent test using Playwright with Firefox.
 *
 * <p>
 * Supports both duration-based runs (e.g. 2h, 8h) and cycle-based runs, with flexible parallel browsers:
 * <ul>
 *   <li><b>Duration-based Execution</b>: Set {@code -Dtest.duration=2h} (or 8h, 30m, 120s).
 *       Long-lived users remain open and active for the entire duration. Short and mid users cycle continuously.</li>
 *   <li><b>User Pool Selection</b>: With 10 database accounts, if parallel count &gt; 10 (e.g. 20 parallel browsers),
 *       users are selected randomly from the available set. If &lt;= 10, unique users are assigned 1-to-1.</li>
 *   <li><b>User Personas</b>:
 *     <ul>
 *       <li><b>Short-Lived</b>: Opens browser, logs in, performs fast navigation, logs out / abrupt close, quick finish.</li>
 *       <li><b>Mid-Lived</b>: Multi-round navigation with dwell; select users open 2 tabs in the same session; mixed logout vs abrupt close.</li>
 *       <li><b>Long-Lived</b>: Opens browser, logs in, navigates with dwell, and stays connected with browser open until the overall test execution stops.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>
 * Configurable properties (via constants or JVM system properties):
 * <ul>
 *   <li>{@code test.duration} - Total run duration (e.g. "2h", "8h", "45m", "300s"). Overrides test.cycles when set.</li>
 *   <li>{@code test.parallel.count} or {@code test.browser.count} - Number of concurrent browser slots (default: 10, e.g. 20)</li>
 *   <li>{@code test.cycles} or {@code test.total.loops} - Number of full cycles in cycle mode (default: 1; -1 for infinite)</li>
 *   <li>{@code test.base.url} - Target URL (default: http://localhost:8080)</li>
 *   <li>{@code test.headless} - Run headless or visual (default: false if DISPLAY available, true otherwise)</li>
 *   <li>{@code test.login.enabled} - Whether to perform login (default: true)</li>
 *   <li>{@code test.username} - Specific single username override</li>
 *   <li>{@code test.password} - Specific single password override</li>
 *   <li>{@code test.view.stay.time.ms} - Dwell time per view in ms (default: 1500)</li>
 *   <li>{@code test.logout.enabled} - Whether logout is permitted (default: true)</li>
 *   <li>{@code test.post.dwell.ms} - Dwell time before logout/finish in ms (default: 3000)</li>
 * </ul>
 * </p>
 */
public class MultiBrowserFirefoxTest {

    // User credentials record
    public record TestUser(String username, String password) {}

    // View navigation timing
    public record ViewTiming(String viewName, long durationMs) {}

    /**
     * Behavioral user personas representing different lifecycle patterns.
     */
    public enum UserPersona {
        SHORT_LIVED("Short-Lived"),
        MID_LIVED("Mid-Lived"),
        LONG_LIVED("Long-Lived");

        private final String label;

        UserPersona(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /**
     * User execution profile specifying credentials, persona, multi-tab usage, and logout behavior.
     */
    public record UserProfile(
            TestUser user,
            UserPersona persona,
            boolean openMultipleTabs,
            boolean performLogout,
            int viewStayTimeMs,
            int navRounds
    ) {}

    /**
     * Detailed timing and performance metrics report for a single browser test session.
     */
    public static class BrowserSessionReport {
        private final int testNumber;
        private final String user;
        private final String label;
        private final UserPersona persona;
        private int tabCount = 1;
        private boolean performedLogout = false;
        private final LocalDateTime startDateTime;
        private LocalDateTime endDateTime;
        private long loginTimeMs = -1;
        private long dashEntryTimeMs = -1;
        private final List<ViewTiming> viewTimings = new ArrayList<>();
        private long dashExitTimeMs = -1;
        private long logoutTimeMs = -1;
        private long totalDurationMs = -1;
        private boolean success = false;
        private String error = null;

        public BrowserSessionReport(int testNumber, String user, String label, UserPersona persona) {
            this.testNumber = testNumber;
            this.user = user;
            this.label = label;
            this.persona = persona;
            this.startDateTime = LocalDateTime.now();
        }

        public int testNumber() { return testNumber; }
        public String user() { return user; }
        public String label() { return label; }
        public UserPersona persona() { return persona; }
        public int tabCount() { return tabCount; }
        public void setTabCount(int tabCount) { this.tabCount = tabCount; }
        public boolean performedLogout() { return performedLogout; }
        public void setPerformedLogout(boolean performedLogout) { this.performedLogout = performedLogout; }
        public LocalDateTime startDateTime() { return startDateTime; }
        public LocalDateTime endDateTime() { return endDateTime; }
        public void setEndDateTime(LocalDateTime dt) { this.endDateTime = dt; }
        public long loginTimeMs() { return loginTimeMs; }
        public void setLoginTimeMs(long ms) { this.loginTimeMs = ms; }
        public long dashEntryTimeMs() { return dashEntryTimeMs; }
        public void setDashEntryTimeMs(long ms) { this.dashEntryTimeMs = ms; }
        public List<ViewTiming> viewTimings() { return viewTimings; }
        public void addViewTiming(String view, long ms) { this.viewTimings.add(new ViewTiming(view, ms)); }
        public long dashExitTimeMs() { return dashExitTimeMs; }
        public void setDashExitTimeMs(long ms) { this.dashExitTimeMs = ms; }
        public long logoutTimeMs() { return logoutTimeMs; }
        public void setLogoutTimeMs(long ms) { this.logoutTimeMs = ms; }
        public long totalDurationMs() { return totalDurationMs; }
        public void setTotalDurationMs(long ms) { this.totalDurationMs = ms; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean s) { this.success = s; }
        public String error() { return error; }
        public void setError(String e) { this.error = e; }

        public long totalViewsNavTimeMs() {
            return viewTimings.stream().mapToLong(ViewTiming::durationMs).sum();
        }

        public double averageViewNavTimeMs() {
            return viewTimings.isEmpty() ? 0.0 : (double) totalViewsNavTimeMs() / viewTimings.size();
        }
    }

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter SHORT_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /**
     * Unique 10 user accounts registered in CustomIdentityProvider.
     */
    public static final List<TestUser> TEST_USERS = List.of(
            new TestUser("admin", "admin"),
            new TestUser("manager", "manager"),
            new TestUser("supervisor", "supervisor"),
            new TestUser("cashier", "cashier"),
            new TestUser("user", "user"),
            new TestUser("john", "john"),
            new TestUser("alice", "alice"),
            new TestUser("bob", "bob"),
            new TestUser("clara", "clara"),
            new TestUser("david", "david")
    );

    // =========================================================================
    // CONFIGURATION (Customize directly or override via -Dproperty=value)
    // =========================================================================
    public static final String BASE_URL = System.getProperty("test.base.url", "http://localhost:8080");
    public static final String DEFAULT_USERNAME = System.getProperty("test.username", "");
    public static final String DEFAULT_PASSWORD = System.getProperty("test.password", "");

    // Duration-based test execution: e.g. "2h", "8h", "30m", "120s"
    public static final String TEST_DURATION_STR = System.getProperty("test.duration", "").trim();
    public static final long TEST_DURATION_MS = parseDurationToMillis(TEST_DURATION_STR);

    // Number of concurrent parallel browser slots (default: 10, can be 20, 50, etc.)
    public static final int NUM_BROWSERS = Integer.getInteger(
            "test.parallel.count",
            Integer.getInteger("test.browser.count", DEFAULT_USERNAME.isEmpty() ? 10 : 1)
    );

    // Visual mode by default if GUI display exists, or headless in headless environments
    public static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty(
            "test.headless",
            (System.getenv("DISPLAY") == null && System.getenv("WAYLAND_DISPLAY") == null) ? "true" : "false"
    ));

    public static final boolean ENABLE_LOGIN = Boolean.parseBoolean(System.getProperty("test.login.enabled", "true"));

    // Timing (easily adjustable)
    public static final int VIEW_STAY_TIME_MS = Integer.getInteger("test.view.stay.time.ms", 1500);
    public static final int POST_NAVIGATION_DWELL_MS = Integer.getInteger("test.post.dwell.ms", 3000);
    public static final boolean ENABLE_LOGOUT = Boolean.parseBoolean(System.getProperty("test.logout.enabled", "true"));

    // Number of full test cycles to execute if duration mode is not specified (default: 1; -1 or <= 0 for infinite)
    public static final int TOTAL_CYCLES = Integer.getInteger(
            "test.cycles",
            Integer.getInteger("test.total.loops", TEST_DURATION_MS > 0 ? -1 : 1)
    );
    public static final int TOTAL_LOOPS = TOTAL_CYCLES;

    // Navigation views (matching MainLayout side menu)
    public static final List<String> ALL_VIEWS = List.of(
            "Dashboard",
            "Contacts",
            "Deals",
            "Tasks",
            "Calendar",
            "Reports"
    );

    private static final Page.NavigateOptions NAV_OPTIONS = new Page.NavigateOptions()
            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            .setTimeout(60000);

    // Global session counter and active flag for graceful termination
    private static final AtomicInteger GLOBAL_SESSION_COUNTER = new AtomicInteger(0);
    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean(true);

    public static void main(String[] args) {
        long testStartTime = System.currentTimeMillis();
        long testEndTime = (TEST_DURATION_MS > 0) ? (testStartTime + TEST_DURATION_MS) : Long.MAX_VALUE;

        System.out.println("=================================================");
        System.out.println("=== Starting Multi-Browser Firefox Test ===");
        System.out.println("Target URL:         " + BASE_URL);
        System.out.println("Parallel browsers:  " + NUM_BROWSERS);
        if (TEST_DURATION_MS > 0) {
            System.out.println("Execution mode:     DURATION-BASED (" + TEST_DURATION_STR + " / " + (TEST_DURATION_MS / 1000) + "s)");
            System.out.println("Target end time:    " + DATE_TIME_FMT.format(LocalDateTime.now().plusSeconds(TEST_DURATION_MS / 1000)));
        } else {
            System.out.println("Execution mode:     CYCLE-BASED (" + (TOTAL_CYCLES <= 0 ? "INFINITE (-1)" : TOTAL_CYCLES + " cycles") + ")");
        }
        System.out.println("Database accounts:  " + TEST_USERS.size() + " accounts available ("
                + (NUM_BROWSERS > TEST_USERS.size() ? "random selection from pool for " + NUM_BROWSERS + " parallel" : "unique 1-to-1 mapping") + ")");
        System.out.println("Headless:           " + HEADLESS);
        System.out.println("Login check:        " + (ENABLE_LOGIN ? "ENABLED" : "DISABLED"));
        System.out.println("View dwell time:    " + VIEW_STAY_TIME_MS + " ms");
        System.out.println("Logout enabled:     " + ENABLE_LOGOUT);
        System.out.println("Personas active:    Short-Lived (fast), Mid-Lived (multi-tab/wait), Long-Lived (stay open until test end)");
        System.out.println("=================================================");

        final List<BrowserSessionReport> allReports = Collections.synchronizedList(new ArrayList<>());

        // Graceful shutdown hook for Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            IS_RUNNING.set(false);
            System.out.println("\n[SHUTDOWN] Interruption caught. Generating final report of completed sessions...");
            printSessionReportsTable(allReports, "FINAL PERFORMANCE REPORT (INTERRUPTED)");
        }));

        try {
            runConcurrentBrowserTests(testStartTime, testEndTime, allReports);
        } catch (Exception ex) {
            System.getLogger(MultiBrowserFirefoxTest.class.getName())
                    .log(System.Logger.Level.ERROR, "Fatal test error", ex);
            System.exit(1);
        }
    }

    /**
     * Parses duration strings like "2h", "8h", "30m", "120s", or plain seconds.
     */
    private static long parseDurationToMillis(String durationStr) {
        if (durationStr == null || durationStr.isBlank()) {
            return 0L;
        }
        String s = durationStr.trim().toLowerCase();
        try {
            if (s.endsWith("h")) {
                long hours = Long.parseLong(s.substring(0, s.length() - 1).trim());
                return hours * 3600_000L;
            } else if (s.endsWith("m")) {
                long mins = Long.parseLong(s.substring(0, s.length() - 1).trim());
                return mins * 60_000L;
            } else if (s.endsWith("s")) {
                long secs = Long.parseLong(s.substring(0, s.length() - 1).trim());
                return secs * 1000L;
            } else if (s.endsWith("d")) {
                long days = Long.parseLong(s.substring(0, s.length() - 1).trim());
                return days * 86400_000L;
            } else {
                return Long.parseLong(s) * 1000L;
            }
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid test.duration '" + durationStr + "', defaulting to cycle mode.");
            return 0L;
        }
    }

    private static boolean isTestTimeExpired(long testEndTime) {
        return TEST_DURATION_MS > 0 && System.currentTimeMillis() >= testEndTime;
    }

    /**
     * Resolves user credentials. If parallel browsers > 10, selects randomly from the database set.
     */
    private static TestUser resolveUser(int taskIndex) {
        if (!DEFAULT_USERNAME.isEmpty()) {
            return new TestUser(DEFAULT_USERNAME, DEFAULT_PASSWORD.isEmpty() ? DEFAULT_USERNAME : DEFAULT_PASSWORD);
        }
        if (NUM_BROWSERS <= TEST_USERS.size()) {
            return TEST_USERS.get((taskIndex - 1) % TEST_USERS.size());
        } else {
            // Randomly select from the set of 10 users
            int randomIdx = ThreadLocalRandom.current().nextInt(TEST_USERS.size());
            return TEST_USERS.get(randomIdx);
        }
    }

    /**
     * Allocates personas across the parallel slots (approx 20% long-lived, 40% mid-lived, 40% short-lived).
     */
    private static UserPersona resolvePersonaForSlot(int slotIndex, int totalParallel) {
        if (totalParallel <= 1) {
            return UserPersona.SHORT_LIVED;
        }
        int longCount = Math.max(1, (int) Math.round(totalParallel * 0.20));
        int shortCount = Math.max(1, (int) Math.round((totalParallel - longCount) * 0.50));

        if (slotIndex <= shortCount) {
            return UserPersona.SHORT_LIVED;
        } else if (slotIndex <= (shortCount + (totalParallel - longCount - shortCount))) {
            return UserPersona.MID_LIVED;
        } else {
            return UserPersona.LONG_LIVED;
        }
    }

    /**
     * Resolves a user profile for a given slot and session iteration.
     */
    private static UserProfile resolveProfile(int slotIndex, int totalParallel) {
        TestUser user = resolveUser(slotIndex);
        UserPersona persona = resolvePersonaForSlot(slotIndex, totalParallel);

        boolean openMultipleTabs;
        boolean performLogout;
        int dwellMs;
        int navRounds;

        switch (persona) {
            case SHORT_LIVED -> {
                openMultipleTabs = false;
                // Approx 75% logout, 25% abrupt close
                performLogout = ENABLE_LOGOUT && (slotIndex % 4 != 0);
                dwellMs = Math.max(500, VIEW_STAY_TIME_MS / 2);
                navRounds = 1;
            }
            case MID_LIVED -> {
                // Half open 2 tabs, half single tab
                openMultipleTabs = (slotIndex % 2 == 1);
                // 50% logout, 50% abrupt close
                performLogout = ENABLE_LOGOUT && (slotIndex % 2 == 0);
                dwellMs = VIEW_STAY_TIME_MS;
                navRounds = 2;
            }
            case LONG_LIVED -> {
                openMultipleTabs = (slotIndex % 2 == 1);
                performLogout = ENABLE_LOGOUT && (slotIndex % 2 == 0);
                dwellMs = VIEW_STAY_TIME_MS;
                navRounds = 1;
            }
            default -> {
                openMultipleTabs = false;
                performLogout = ENABLE_LOGOUT;
                dwellMs = VIEW_STAY_TIME_MS;
                navRounds = 1;
            }
        }

        return new UserProfile(user, persona, openMultipleTabs, performLogout, dwellMs, navRounds);
    }

    private static void runConcurrentBrowserTests(long testStartTime, long testEndTime, List<BrowserSessionReport> allReports) {
        // Mode 1: Duration-based continuous execution
        if (TEST_DURATION_MS > 0) {
            runDurationBasedExecution(testEndTime, allReports);
        } else {
            // Mode 2: Cycle-based execution
            runCycleBasedExecution(allReports);
        }

        printSessionReportsTable(allReports, "OVERALL PERFORMANCE REPORT: ALL SESSIONS (" + allReports.size() + " total)");
    }

    /**
     * Duration-based execution:
     * Long-lived slots remain open and active until the full duration ends.
     * Short and mid slots cycle continuously until the full duration ends.
     */
    private static void runDurationBasedExecution(long testEndTime, List<BrowserSessionReport> allReports) {
        System.out.println("\n" + "=".repeat(160));
        System.out.println("=== Starting Continuous Duration Execution (" + TEST_DURATION_STR + " / " + (TEST_DURATION_MS / 1000)
                + "s) with " + NUM_BROWSERS + " parallel browser slots ===");
        System.out.println("=".repeat(160));

        final CountDownLatch initialLoginBarrier = new CountDownLatch(NUM_BROWSERS);
        List<Thread> slotThreads = new ArrayList<>();

        for (int i = 1; i <= NUM_BROWSERS; i++) {
            final int slotId = i;
            final UserPersona persona = resolvePersonaForSlot(slotId, NUM_BROWSERS);

            Thread vThread = Thread.ofVirtual().start(() -> {
                if (slotId > 1) {
                    sleep((slotId - 1) * 250L);
                }

                if (persona == UserPersona.LONG_LIVED) {
                    // Long-lived slot: opens browser, logs in, stays open until testEndTime
                    int testNum = GLOBAL_SESSION_COUNTER.incrementAndGet();
                    UserProfile profile = resolveProfile(slotId, NUM_BROWSERS);
                    String label = "Slot#" + slotId + "[Test#" + testNum + "|" + profile.user().username() + "|LONG_LIVED"
                            + (profile.openMultipleTabs() ? "|2Tabs" : "") + "]";
                    Thread.currentThread().setName(label);

                    System.out.println("[" + label + "] Long-lived session starting. Will stay open until "
                            + DATE_TIME_FMT.format(LocalDateTime.now().plusSeconds(Math.max(0, (testEndTime - System.currentTimeMillis()) / 1000))));

                    BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(HEADLESS);
                    try (Playwright playwright = Playwright.create();
                         Browser browser = playwright.firefox().launch(launchOptions);
                         BrowserContext context = browser.newContext()) {

                        BrowserSessionReport report = executeBrowserSession(
                                context, label, testNum, profile, initialLoginBarrier, testEndTime, null);
                        allReports.add(report);
                        System.out.println("[" + label + "] Long-lived session concluded. Total online duration: "
                                + report.totalDurationMs() + " ms");
                    } catch (Exception e) {
                        System.err.println("[" + label + "] Failed: " + e.getMessage());
                    }
                } else {
                    // Short / Mid-lived slot: loops continuously until testEndTime
                    int sessionInSlot = 0;
                    while (IS_RUNNING.get() && !isTestTimeExpired(testEndTime)) {
                        sessionInSlot++;
                        int testNum = GLOBAL_SESSION_COUNTER.incrementAndGet();
                        UserProfile profile = resolveProfile(slotId, NUM_BROWSERS);
                        String label = "Slot#" + slotId + "[Test#" + testNum + "|" + profile.user().username()
                                + "|" + profile.persona().label()
                                + (profile.openMultipleTabs() ? "|2Tabs" : "")
                                + (profile.performLogout() ? "|Logout" : "|Abrupt")
                                + "]";
                        Thread.currentThread().setName(label);

                        // Only synchronize barrier on the very first round of the test
                        CountDownLatch barrier = (sessionInSlot == 1) ? initialLoginBarrier : null;

                        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(HEADLESS);
                        try (Playwright playwright = Playwright.create();
                             Browser browser = playwright.firefox().launch(launchOptions);
                             BrowserContext context = browser.newContext()) {

                            BrowserSessionReport report = executeBrowserSession(
                                    context, label, testNum, profile, barrier, testEndTime, null);
                            allReports.add(report);
                            System.out.println("[" + label + "] Finished session in " + report.totalDurationMs()
                                    + " ms. (Completed sessions so far: " + allReports.size() + ")");
                        } catch (Exception e) {
                            System.err.println("[" + label + "] Failed: " + e.getMessage());
                        }

                        // Stagger before next session in this slot
                        if (IS_RUNNING.get() && !isTestTimeExpired(testEndTime)) {
                            sleep(1000L);
                        }
                    }
                    System.out.println("[Slot#" + slotId + "] Completed all sessions for duration. Total slot runs: " + sessionInSlot);
                }
            });
            slotThreads.add(vThread);
        }

        for (Thread t : slotThreads) {
            try {
                t.join();
            } catch (InterruptedException ex) {
                System.getLogger(MultiBrowserFirefoxTest.class.getName())
                        .log(System.Logger.Level.ERROR, "Thread join interrupted", ex);
            }
        }
    }

    /**
     * Cycle-based execution: runs TOTAL_CYCLES full cycles.
     */
    private static void runCycleBasedExecution(List<BrowserSessionReport> allReports) {
        long totalLoop = 0;

        while (TOTAL_CYCLES <= 0 || totalLoop < TOTAL_CYCLES) {
            long currentLoop = totalLoop + 1;
            LocalDateTime loopStartTime = LocalDateTime.now();
            System.out.println("\n" + "=".repeat(160));
            System.out.println("=== Starting Test Cycle " + currentLoop + (TOTAL_CYCLES > 0 ? "/" + TOTAL_CYCLES : " (infinite)")
                    + " (" + NUM_BROWSERS + " parallel slots) | Started: " + DATE_TIME_FMT.format(loopStartTime) + " ===");
            System.out.println("=".repeat(160));

            final CountDownLatch loginBarrier = new CountDownLatch(NUM_BROWSERS);

            int shortMidCount = 0;
            for (int i = 1; i <= NUM_BROWSERS; i++) {
                if (resolvePersonaForSlot(i, NUM_BROWSERS) != UserPersona.LONG_LIVED) {
                    shortMidCount++;
                }
            }
            final CountDownLatch shortMidDoneLatch = new CountDownLatch(shortMidCount);

            List<Thread> threads = new ArrayList<>();
            List<BrowserSessionReport> loopReports = Collections.synchronizedList(new ArrayList<>());

            for (int i = 1; i <= NUM_BROWSERS; i++) {
                final int taskId = i;
                final UserProfile profile = resolveProfile(taskId, NUM_BROWSERS);
                Thread vThread = Thread.ofVirtual().start(() -> {
                    int testNum = GLOBAL_SESSION_COUNTER.incrementAndGet();
                    String label = "C" + currentLoop + "-Slot#" + taskId + "[Test#" + testNum + "|" + profile.user().username()
                            + "|" + profile.persona().label()
                            + (profile.openMultipleTabs() ? "|2Tabs" : "")
                            + (profile.performLogout() ? "|Logout" : "|Abrupt")
                            + "]";
                    Thread.currentThread().setName(label);

                    if (taskId > 1) {
                        sleep((taskId - 1) * 250L);
                    }

                    BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(HEADLESS);

                    try (Playwright playwright = Playwright.create();
                         Browser browser = playwright.firefox().launch(launchOptions);
                         BrowserContext context = browser.newContext()) {

                        BrowserSessionReport report = executeBrowserSession(
                                context, label, testNum, profile, loginBarrier, Long.MAX_VALUE, shortMidDoneLatch);
                        loopReports.add(report);
                    } catch (Exception e) {
                        System.err.println("[" + label + "] FAILED: " + e.getMessage());
                    } finally {
                        if (profile.persona() != UserPersona.LONG_LIVED) {
                            shortMidDoneLatch.countDown();
                        }
                    }
                });
                threads.add(vThread);
            }

            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException ex) {
                    System.getLogger(MultiBrowserFirefoxTest.class.getName())
                            .log(System.Logger.Level.ERROR, "Exception on thread join", ex);
                }
            }

            totalLoop++;
            allReports.addAll(loopReports);
            printSessionReportsTable(loopReports, "PERFORMANCE REPORT: TEST CYCLE " + currentLoop);
        }
    }

    private static BrowserSessionReport executeBrowserSession(
            BrowserContext context,
            String label,
            int testNumber,
            UserProfile profile,
            CountDownLatch loginBarrier,
            long testEndTime,
            CountDownLatch cycleShortMidDoneLatch) {

        BrowserSessionReport report = new BrowserSessionReport(testNumber, profile.user().username(), label, profile.persona());
        report.setTabCount(profile.openMultipleTabs() ? 2 : 1);
        long sessionStart = System.currentTimeMillis();

        Page page = context.newPage();
        setupConsoleLogging(page, label + "-Tab1");
        Page tab2 = null;

        try {
            // 1. Navigate to base URL on Tab 1
            System.out.println("[" + label + "] Navigating to " + BASE_URL);
            page.navigate(BASE_URL, NAV_OPTIONS);

            // 2. Perform Login on Tab 1 if enabled/required
            if (ENABLE_LOGIN) {
                long loginDuration = performLoginIfRequired(page, label, profile.user());
                report.setLoginTimeMs(loginDuration);
            }

            // 3. Ensure Dashboard is loaded on Tab 1 (DashEntry)
            long dashEntryDuration = waitForDashboard(page, label);
            report.setDashEntryTimeMs(dashEntryDuration);

            // 4. Synchronize all parallel users on initial startup
            if (loginBarrier != null && NUM_BROWSERS > 1) {
                loginBarrier.countDown();
                long remaining = loginBarrier.getCount();
                if (remaining > 0) {
                    System.out.println("[" + label + "] Online! Waiting for parallel users ("
                            + (NUM_BROWSERS - remaining) + "/" + NUM_BROWSERS + " online)...");
                }
                try {
                    loginBarrier.await(45, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // 5. If multi-tab user: open Tab 2 sharing the session context
            if (profile.openMultipleTabs()) {
                System.out.println("[" + label + "] Multi-tab enabled: Opening Tab 2 in shared session...");
                try {
                    tab2 = context.newPage();
                    setupConsoleLogging(tab2, label + "-Tab2");
                    tab2.navigate(BASE_URL, NAV_OPTIONS);
                    waitForDashboard(tab2, label + "-Tab2");
                    System.out.println("[" + label + "] Tab 2 ready on Dashboard.");
                } catch (Exception e) {
                    System.err.println("[" + label + "] Notice on Tab 2 init: " + e.getMessage());
                }
            }

            // 6. Navigate through views according to profile.navRounds()
            for (int round = 1; round <= profile.navRounds(); round++) {
                if (profile.navRounds() > 1) {
                    System.out.println("[" + label + "] Starting navigation round " + round + "/" + profile.navRounds() + "...");
                }
                for (String viewName : ALL_VIEWS) {
                    if ("Dashboard".equalsIgnoreCase(viewName)) {
                        continue;
                    }
                    long navDuration = navigateToView(page, viewName, label);
                    report.addViewTiming(viewName, navDuration);

                    // If Tab 2 is active, navigate Tab 2 to a complementary view
                    if (tab2 != null) {
                        try {
                            String tab2View = "Reports".equalsIgnoreCase(viewName) ? "Contacts" : "Reports";
                            navigateToView(tab2, tab2View, label + "-Tab2");
                        } catch (Throwable ignored) {
                        }
                    }

                    sleep(profile.viewStayTimeMs());
                }
            }

            // 7. Return to Dashboard (Dashboard Exit)
            long dashExitDuration = navigateBackToDashboard(page, label);
            report.setDashExitTimeMs(dashExitDuration);

            // 8. Persona-specific sustain/wait behavior:
            if (profile.persona() == UserPersona.LONG_LIVED) {
                if (TEST_DURATION_MS > 0) {
                    // Duration mode: remain open and active until the entire test duration expires
                    System.out.println("[" + label + "] (LONG_LIVED) Connected and active. Remaining open until test duration ends...");
                    while (IS_RUNNING.get() && !isTestTimeExpired(testEndTime)) {
                        long remainingMs = testEndTime - System.currentTimeMillis();
                        long sleepTime = Math.min(Math.max(remainingMs, 0), 15000L);
                        if (sleepTime <= 0) {
                            break;
                        }
                        sleep(sleepTime);
                        // Lightweight presence check to maintain session WebSocket alive
                        if (IS_RUNNING.get() && !isTestTimeExpired(testEndTime)) {
                            try {
                                page.title();
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                    System.out.println("[" + label + "] (LONG_LIVED) Test duration reached. Concluding session...");
                } else if (cycleShortMidDoneLatch != null) {
                    // Cycle mode: wait until short/mid sessions in cycle finish
                    System.out.println("[" + label + "] (LONG_LIVED) Staying open until all short and mid sessions in cycle complete...");
                    try {
                        cycleShortMidDoneLatch.await(10, TimeUnit.MINUTES);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } else if (profile.persona() == UserPersona.MID_LIVED && POST_NAVIGATION_DWELL_MS > 0) {
                System.out.println("[" + label + "] Dwell wait of " + POST_NAVIGATION_DWELL_MS + " ms before closing...");
                sleep(POST_NAVIGATION_DWELL_MS);
            }

            // Close Tab 2 if opened
            if (tab2 != null) {
                try {
                    tab2.close();
                } catch (Throwable ignored) {
                }
            }

            // 9. Logout vs Abrupt Close
            if (profile.performLogout()) {
                System.out.println("[" + label + "] Performing explicit clean logout...");
                long logoutDuration = performLogout(page, label);
                report.setLogoutTimeMs(logoutDuration);
                report.setPerformedLogout(true);
            } else {
                report.setPerformedLogout(false);
                System.out.println("[" + label + "] Skipping logout: closing browser abruptly (simulating direct window/tab close).");
            }

            report.setSuccess(true);
        } catch (Exception e) {
            report.setSuccess(false);
            report.setError(e.getMessage());
            throw e;
        } finally {
            report.setEndDateTime(LocalDateTime.now());
            report.setTotalDurationMs(System.currentTimeMillis() - sessionStart);
        }

        System.out.println("[" + label + "] Closed browser session.");
        return report;
    }

    private static long performLoginIfRequired(Page page, String label, TestUser user) {
        System.out.println("[" + label + "] Waiting for initial page load (login dialog or dashboard)...");

        Locator appOrLogin = page.locator("dwc-login, dwc-app-nav-item").first();
        try {
            appOrLogin.waitFor(new Locator.WaitForOptions().setTimeout(30000));
        } catch (Exception e) {
            System.out.println("[" + label + "] Initial component wait timed out, checking current state...");
        }

        Locator loginDialog = page.locator("dwc-login");
        boolean loginNeeded = false;
        try {
            loginNeeded = page.url().contains("/login") || loginDialog.isVisible();
        } catch (Throwable ignored) {
        }

        if (ENABLE_LOGIN && loginNeeded) {
            System.out.println("[" + label + "] Login dialog detected. Authenticating as '" + user.username() + "'...");
            long start = System.currentTimeMillis();
            try {
                Locator usernameField = page.locator("dwc-login input:not([type='password']), input:not([type='password'])").first();
                usernameField.waitFor(new Locator.WaitForOptions().setTimeout(15000));
                usernameField.click();
                usernameField.fill(user.username());

                Locator passwordField = page.locator("dwc-login input[type='password'], input[type='password']").first();
                passwordField.click();
                passwordField.fill(user.password());

                Locator submitButton = page.locator("dwc-login button, dwc-button:has-text('Sign in'), button:has-text('Sign in'), [part='submit-button']").first();
                if (submitButton.isVisible()) {
                    submitButton.click();
                } else {
                    passwordField.press("Enter");
                }

                System.out.println("[" + label + "] Credentials submitted for '" + user.username() + "'.");
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("[" + label + "] Login submission completed in " + elapsed + " ms.");
                return elapsed;
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - start;
                System.err.println("[" + label + "] Error submitting login for '" + user.username() + "' after " + elapsed + " ms: " + e.getMessage());
                return elapsed;
            }
        } else {
            System.out.println("[" + label + "] Login not required (already on dashboard or login disabled).");
            return 0;
        }
    }

    private static long waitForDashboard(Page page, String label) {
        System.out.println("[" + label + "] Waiting for dashboard side-nav...");
        long start = System.currentTimeMillis();
        try {
            page.locator("dwc-app-nav-item").first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(30000));
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + label + "] Dashboard ready in " + elapsed + " ms. Title: '" + page.title() + "' | URL: " + page.url());
            return elapsed;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            System.err.println("[" + label + "] Warning: Dashboard locator wait timed out after " + elapsed + " ms: " + e.getMessage());
            return elapsed;
        }
    }

    private static long navigateToView(Page page, String viewName, String label) {
        System.out.println("[" + label + "] Navigating to view '" + viewName + "'...");
        long start = System.currentTimeMillis();
        try {
            Locator navItem = page.locator("dwc-app-nav-item:has-text('" + viewName + "')").first();
            navItem.waitFor(new Locator.WaitForOptions().setTimeout(15000));
            navItem.click();
            sleep(300);
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + label + "] -> Entered '" + viewName + "' in " + elapsed + " ms. Title: '" + page.title() + "' | URL: " + page.url());
            return elapsed;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            System.err.println("[" + label + "] Failed navigating to '" + viewName + "' after " + elapsed + " ms: " + e.getMessage());
            return elapsed;
        }
    }

    private static long navigateBackToDashboard(Page page, String label) {
        System.out.println("[" + label + "] Navigating back to Dashboard (Dashboard Exit)...");
        long start = System.currentTimeMillis();
        try {
            Locator navItem = page.locator("dwc-app-nav-item:has-text('Dashboard')").first();
            navItem.waitFor(new Locator.WaitForOptions().setTimeout(15000));
            navItem.click();
            sleep(300);
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + label + "] -> Returned to Dashboard in " + elapsed + " ms. Title: '" + page.title() + "' | URL: " + page.url());
            return elapsed;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            System.err.println("[" + label + "] Failed returning to Dashboard after " + elapsed + " ms: " + e.getMessage());
            return elapsed;
        }
    }

    private static long performLogout(Page page, String label) {
        System.out.println("[" + label + "] Performing Logout...");
        long start = System.currentTimeMillis();
        try {
            Locator logoutBtn = page.locator("dwc-icon-button[name='logout'], [tooltip-text='Log out'], dwc-icon-button:has([name='logout'])").first();
            logoutBtn.waitFor(new Locator.WaitForOptions().setTimeout(15000));
            logoutBtn.click();
            System.out.println("[" + label + "] Clicked logout button. Waiting for login redirection...");

            try {
                page.locator("dwc-login").first().waitFor(new Locator.WaitForOptions().setTimeout(15000));
            } catch (Throwable ignored) {
            }
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + label + "] Logout complete in " + elapsed + " ms. Current URL: " + page.url());
            return elapsed;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            System.err.println("[" + label + "] Logout notice after " + elapsed + " ms: " + e.getMessage());
            return elapsed;
        }
    }

    private static void printSessionReportsTable(List<BrowserSessionReport> reports, String title) {
        if (reports == null || reports.isEmpty()) {
            return;
        }

        LocalDateTime earliestStart = reports.stream()
                .map(BrowserSessionReport::startDateTime)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        LocalDateTime latestEnd = reports.stream()
                .map(r -> r.endDateTime() != null ? r.endDateTime() : r.startDateTime())
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        System.out.println("\n" + "=".repeat(160));
        System.out.println("=== " + title + " ===");
        System.out.println("Started: " + DATE_TIME_FMT.format(earliestStart) + " | Finished: " + DATE_TIME_FMT.format(latestEnd)
                + " | Total Sessions Completed: " + reports.size());
        System.out.println("=".repeat(160));
        System.out.printf("%-6s | %-10s | %-11s | %-4s | %-12s | %-12s | %-6s | %-11s | %-11s | %-24s | %-11s | %-15s | %-11s%n",
                "Test#", "User", "Persona", "Tabs", "Start Time", "End Time", "Status", "Login Time", "Dash Entry", "Views Nav (Avg / Tot)", "Dash Exit", "Logout Action", "Total Time");
        System.out.println("-".repeat(160));

        long totalLogin = 0, countLogin = 0;
        long totalDashEntry = 0, countDashEntry = 0;
        long totalViewNav = 0, countViews = 0;
        long totalDashExit = 0, countDashExit = 0;
        long totalLogout = 0, countLogout = 0;
        long totalDurationSum = 0;

        for (BrowserSessionReport r : reports) {
            String status = r.isSuccess() ? "OK" : "FAIL";
            String startStr = SHORT_TIME_FMT.format(r.startDateTime());
            String endStr = r.endDateTime() != null ? SHORT_TIME_FMT.format(r.endDateTime()) : "IN-FLIGHT";
            String loginStr = r.loginTimeMs() >= 0 ? r.loginTimeMs() + " ms" : "N/A";
            String dashEntryStr = r.dashEntryTimeMs() >= 0 ? r.dashEntryTimeMs() + " ms" : "N/A";
            String viewsStr = String.format("%.0f ms (%d views)", r.averageViewNavTimeMs(), r.viewTimings().size());
            String dashExitStr = r.dashExitTimeMs() >= 0 ? r.dashExitTimeMs() + " ms" : "N/A";
            String logoutStr = r.performedLogout()
                    ? (r.logoutTimeMs() >= 0 ? r.logoutTimeMs() + " ms" : "OK")
                    : "ABRUPT CLOSE";
            String totalStr = r.totalDurationMs() >= 0 ? r.totalDurationMs() + " ms" : "N/A";

            System.out.printf("#%-5d | %-10s | %-11s | %-4d | %-12s | %-12s | %-6s | %11s | %11s | %24s | %11s | %15s | %11s%n",
                    r.testNumber(), r.user(), r.persona().label(), r.tabCount(), startStr, endStr, status, loginStr, dashEntryStr, viewsStr, dashExitStr, logoutStr, totalStr);

            if (r.loginTimeMs() >= 0) { totalLogin += r.loginTimeMs(); countLogin++; }
            if (r.dashEntryTimeMs() >= 0) { totalDashEntry += r.dashEntryTimeMs(); countDashEntry++; }
            totalViewNav += r.totalViewsNavTimeMs(); countViews += r.viewTimings().size();
            if (r.dashExitTimeMs() >= 0) { totalDashExit += r.dashExitTimeMs(); countDashExit++; }
            if (r.performedLogout() && r.logoutTimeMs() >= 0) { totalLogout += r.logoutTimeMs(); countLogout++; }
            if (r.totalDurationMs() >= 0) { totalDurationSum += r.totalDurationMs(); }
        }

        System.out.println("-".repeat(160));

        // Sub-summaries per persona
        for (UserPersona persona : UserPersona.values()) {
            List<BrowserSessionReport> pReports = reports.stream()
                    .filter(r -> r.persona() == persona)
                    .toList();
            if (!pReports.isEmpty()) {
                double avgTotal = pReports.stream().mapToLong(BrowserSessionReport::totalDurationMs).average().orElse(0);
                double avgLogin = pReports.stream().filter(r -> r.loginTimeMs() >= 0).mapToLong(BrowserSessionReport::loginTimeMs).average().orElse(0);
                long logoutCount = pReports.stream().filter(BrowserSessionReport::performedLogout).count();
                long abruptCount = pReports.size() - logoutCount;
                System.out.printf("SUB-AVERAGE: %-11s (%d sessions) | Avg Login: %.0f ms | Avg Total: %.0f ms | Logouts: %d, Abrupt Closes: %d%n",
                        persona.label(), pReports.size(), avgLogin, avgTotal, logoutCount, abruptCount);
            }
        }

        System.out.println("-".repeat(160));
        String avgLogin = countLogin > 0 ? (totalLogin / countLogin) + " ms" : "N/A";
        String avgDashEntry = countDashEntry > 0 ? (totalDashEntry / countDashEntry) + " ms" : "N/A";
        String avgViews = countViews > 0 ? String.format("%.0f ms (avg)", (double) totalViewNav / countViews) : "N/A";
        String avgDashExit = countDashExit > 0 ? (totalDashExit / countDashExit) + " ms" : "N/A";
        String avgLogout = countLogout > 0 ? (totalLogout / countLogout) + " ms" : "N/A";
        String avgTotal = (totalDurationSum / reports.size()) + " ms";

        System.out.printf("%-37s | %-12s | %-12s |        | %11s | %11s | %24s | %11s | %15s | %11s%n",
                "OVERALL AVERAGE (" + reports.size() + " sessions)", "", "", avgLogin, avgDashEntry, avgViews, avgDashExit, avgLogout, avgTotal);
        System.out.println("=".repeat(160));

        // Individual view breakdown
        Map<String, List<Long>> perViewMap = new LinkedHashMap<>();
        for (BrowserSessionReport r : reports) {
            for (ViewTiming vt : r.viewTimings()) {
                perViewMap.computeIfAbsent(vt.viewName(), k -> new ArrayList<>()).add(vt.durationMs());
            }
        }
        if (!perViewMap.isEmpty()) {
            System.out.print("Views detail: ");
            List<String> viewParts = new ArrayList<>();
            for (Map.Entry<String, List<Long>> entry : perViewMap.entrySet()) {
                double avg = entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0);
                viewParts.add(entry.getKey() + ": " + String.format("%.0f", avg) + " ms");
            }
            System.out.println(String.join(" | ", viewParts));
            System.out.println("=".repeat(160) + "\n");
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static void setupConsoleLogging(Page page, String browserLabel) {
        page.onConsoleMessage(msg -> {
            String text = msg.text();
            if (text.contains("TypeError") || text.contains("failed") || text.contains("error")
                    || text.contains("Restarting") || text.contains("restarting")) {
                System.out.println("[" + browserLabel + " CONSOLE " + msg.type() + "] " + text);
            }
        });
    }
}
