package io.github.kriolos.opos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
 * Simulates multiple distinct users concurrently online in parallel (1 browser per user).
 *
 * <p>
 * Configurable properties (via constants or JVM system properties):
 * <ul>
 *   <li>{@code test.base.url} - Target URL (default: http://localhost:8080)</li>
 *   <li>{@code test.browser.count} - Number of concurrent browser threads (default: 1 per test user)</li>
 *   <li>{@code test.headless} - Run headless or visual (default: false if DISPLAY available, true otherwise)</li>
 *   <li>{@code test.login.enabled} - Whether to perform login (default: true)</li>
 *   <li>{@code test.username} - Specific single username override (if set, defaults browser count to 1)</li>
 *   <li>{@code test.password} - Specific single password override</li>
 *   <li>{@code test.view.stay.time.ms} - Dwell time per view in ms (default: 1500)</li>
 *   <li>{@code test.logout.enabled} - Whether to perform logout at the end (default: true)</li>
 *   <li>{@code test.post.dwell.ms} - Dwell time before logout/finish in ms (default: 3000)</li>
 * </ul>
 * </p>
 */
public class MultiBrowserFirefoxTest {

    // Security & Login
    public record TestUser(String username, String password) {}

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
            new TestUser("david", "david"),
            
            new TestUser("admin", "admin"),
            new TestUser("manager", "manager"),
            new TestUser("supervisor", "supervisor"),
            new TestUser("cashier", "cashier"),
            new TestUser("user", "user"),
            new TestUser("john", "john"),
            new TestUser("alice", "alice"),
            new TestUser("bob", "bob"),
            new TestUser("clara", "clara"),
            new TestUser("david", "david"),
            
            new TestUser("admin", "admin"),
            new TestUser("manager", "manager"),
            new TestUser("supervisor", "supervisor"),
            new TestUser("cashier", "cashier"),
            new TestUser("user", "user"),
            new TestUser("john", "john"),
            new TestUser("alice", "alice"),
            new TestUser("bob", "bob"),
            new TestUser("clara", "clara"),
            new TestUser("david", "david"),
            
            new TestUser("admin", "admin"),
            new TestUser("manager", "manager"),
            new TestUser("supervisor", "supervisor"),
            new TestUser("cashier", "cashier"),
            new TestUser("user", "user"),
            new TestUser("john", "john"),
            new TestUser("alice", "alice"),
            new TestUser("bob", "bob"),
            new TestUser("clara", "clara"),
            new TestUser("david", "david"),
            
            new TestUser("admin", "admin"),
            new TestUser("manager", "manager"),
            new TestUser("supervisor", "supervisor"),
            new TestUser("cashier", "cashier"),
            new TestUser("user", "user"),
            new TestUser("john", "john"),
            new TestUser("alice", "alice"),
            new TestUser("bob", "bob"),
            new TestUser("clara", "clara"),
            new TestUser("david", "david"),
            
            new TestUser("admin", "admin"),
            new TestUser("manager", "manager"),
            new TestUser("supervisor", "supervisor"),
            new TestUser("cashier", "cashier"),
            new TestUser("user", "user"),
            new TestUser("john", "john"),
            new TestUser("alice", "alice"),
            new TestUser("bob", "bob"),
            new TestUser("clara", "clara"),
            new TestUser("david", "david"),
            
            new TestUser("admin", "admin"),
            new TestUser("manager", "manager"),
            new TestUser("supervisor", "supervisor"),
            new TestUser("cashier", "cashier"),
            new TestUser("user", "user"),
            new TestUser("john", "john"),
            new TestUser("alice", "alice"),
            new TestUser("bob", "bob"),
            new TestUser("clara", "clara"),
            new TestUser("david", "david"),
            
            new TestUser("admin", "admin"),
            new TestUser("manager", "manager"),
            new TestUser("supervisor", "supervisor"),
            new TestUser("cashier", "cashier"),
            new TestUser("user", "user"),
            new TestUser("john", "john"),
            new TestUser("alice", "alice"),
            new TestUser("bob", "bob"),
            new TestUser("clara", "clara"),
            new TestUser("david", "david"),
            
            
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

    // 1 browser per user by default to simulate multi-user online concurrently in parallel
    public static final int NUM_BROWSERS = Integer.getInteger(
            "test.browser.count",
            DEFAULT_USERNAME.isEmpty() ? TEST_USERS.size() : 1
    );

    // Visual mode by default if GUI display exists, or headless in headless environments
    public static final boolean HEADLESS = true ; 
    boolean h = Boolean.parseBoolean(System.getProperty(
            "test.headless",
            (System.getenv("DISPLAY") == null && System.getenv("WAYLAND_DISPLAY") == null) ? "true" : "false"
    ));

    public static final boolean ENABLE_LOGIN = Boolean.parseBoolean(System.getProperty("test.login.enabled", "true"));

    // Timing (easily adjustable)
    public static final int VIEW_STAY_TIME_MS = Integer.getInteger("test.view.stay.time.ms", 1500);
    public static final int POST_NAVIGATION_DWELL_MS = Integer.getInteger("test.post.dwell.ms", 3000);
    public static final boolean ENABLE_LOGOUT = Boolean.parseBoolean(System.getProperty("test.logout.enabled", "true"));

    private static final CountDownLatch LOGIN_BARRIER = new CountDownLatch(NUM_BROWSERS);

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

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("=== Starting Multi-Browser Firefox Test ===");
        System.out.println("Target URL:         " + BASE_URL);
        System.out.println("Browsers count:     " + NUM_BROWSERS);
        System.out.println("Headless:           " + HEADLESS);
        System.out.println("Login check:        " + (ENABLE_LOGIN ? "ENABLED" : "DISABLED"));
        System.out.println("View dwell time:    " + VIEW_STAY_TIME_MS + " ms");
        System.out.println("Logout enabled:     " + ENABLE_LOGOUT);
        System.out.println("Views to visit:     " + String.join(", ", ALL_VIEWS));
        System.out.println("Available users:    " + TEST_USERS.size() + " test accounts configured");
        System.out.println("=================================================");

        try {
            runConcurrentBrowserTests();
        } catch (Exception ex) {
            System.getLogger(MultiBrowserFirefoxTest.class.getName())
                    .log(System.Logger.Level.ERROR, "Fatal test error", ex);
            System.exit(1);
        }
    }

    private static TestUser resolveUser(int taskIndex) {
        if (!DEFAULT_USERNAME.isEmpty()) {
            return new TestUser(DEFAULT_USERNAME, DEFAULT_PASSWORD.isEmpty() ? DEFAULT_USERNAME : DEFAULT_PASSWORD);
        }
        return TEST_USERS.get((taskIndex - 1) % TEST_USERS.size());
    }

    private static void runConcurrentBrowserTests() {
        long totalLoop = 0;
        while (totalLoop < 1_000_000) {
        List<Thread> threads = new ArrayList<>();

        for (int i = 1; i <= NUM_BROWSERS; i++) {
            final int taskId = i;
            final TestUser user = resolveUser(taskId);
            Thread vThread = Thread.ofVirtual().start(() -> {
                String label = "TEST#" + taskId + "[" + user.username() + "]";
                Thread.currentThread().setName(label);

                // Slight stagger for concurrent startups
                if (taskId > 1) {
                    sleep((taskId - 1) * 300L);
                }

                System.out.println("[" + label + "] STARTING (User: " + user.username() + ")...");

                BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                        .setHeadless(HEADLESS);

                try (Playwright playwright = Playwright.create();
                     Browser browser = playwright.firefox().launch(launchOptions);
                     BrowserContext context = browser.newContext()) {

                    executeBrowserSession(context, label, user);
                    System.out.println("[" + label + "] FINISHED SUCCESSFULLY!");

                } catch (Exception e) {
                    System.err.println("[" + label + "] FAILED: " + e.getMessage());
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
        Long totalProcessed = totalLoop * NUM_BROWSERS;
        System.out.println("=== All " + NUM_BROWSERS + " browser session(s) completed ===: "+totalProcessed);
        }
    }

    private static void executeBrowserSession(BrowserContext context, String label, TestUser user) {
        Page page = context.newPage();
        setupConsoleLogging(page, label);

        // 1. Navigate to base URL
        System.out.println("[" + label + "] Navigating to " + BASE_URL);
        page.navigate(BASE_URL, NAV_OPTIONS);

        // 2. Perform Login if enabled/required
        if (ENABLE_LOGIN) {
            performLoginIfRequired(page, label, user);
        }

        // 3. Ensure Dashboard is loaded
        waitForDashboard(page, label);

        // 4. Synchronize all users so they are concurrently online together in parallel
        if (NUM_BROWSERS > 1) {
            LOGIN_BARRIER.countDown();
            long remaining = LOGIN_BARRIER.getCount();
            if (remaining > 0) {
                System.out.println("[" + label + "] Online! Waiting for remaining users to come online ("
                        + (NUM_BROWSERS - remaining) + "/" + NUM_BROWSERS + " online)...");
            }
            try {
                LOGIN_BARRIER.await(45, TimeUnit.SECONDS);
                System.out.println("[" + label + "] All " + NUM_BROWSERS + " users are now CONCURRENTLY ONLINE in parallel! Proceeding to views...");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 5. Navigate through all views
        for (String viewName : ALL_VIEWS) {
            navigateToView(page, viewName, label);
            sleep(VIEW_STAY_TIME_MS);
        }

        // 6. Dwell time before logout while remaining online
        if (POST_NAVIGATION_DWELL_MS > 0) {
            System.out.println("[" + label + "] Staying online for " + POST_NAVIGATION_DWELL_MS + " ms before logout...");
            sleep(POST_NAVIGATION_DWELL_MS);
        }

        // 7. Perform Logout if enabled
        if (ENABLE_LOGOUT) {
            performLogout(page, label);
        }

        System.out.println("[" + label + "] Closing browser session.");
    }

    private static void performLoginIfRequired(Page page, String label, TestUser user) {
        System.out.println("[" + label + "] Waiting for initial page load (login dialog or dashboard)...");

        // Wait up to 30s for either dwc-login or side-nav to appear
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

            try {
                // Wait for the input fields
                Locator usernameField = page.locator("dwc-login input:not([type='password']), input:not([type='password'])").first();
                usernameField.waitFor(new Locator.WaitForOptions().setTimeout(15000));
                usernameField.click();
                usernameField.fill(user.username());

                Locator passwordField = page.locator("dwc-login input[type='password'], input[type='password']").first();
                passwordField.click();
                passwordField.fill(user.password());

                // Submit login
                Locator submitButton = page.locator("dwc-login button, dwc-button:has-text('Sign in'), button:has-text('Sign in'), [part='submit-button']").first();
                if (submitButton.isVisible()) {
                    submitButton.click();
                } else {
                    passwordField.press("Enter");
                }

                System.out.println("[" + label + "] Credentials submitted for '" + user.username() + "'. Waiting for dashboard navigation...");
                sleep(2000);
            } catch (Exception e) {
                System.err.println("[" + label + "] Error submitting login for '" + user.username() + "': " + e.getMessage());
            }
        } else {
            System.out.println("[" + label + "] Login not required (already on dashboard or login disabled).");
        }
    }

    private static void waitForDashboard(Page page, String label) {
        System.out.println("[" + label + "] Waiting for dashboard side-nav...");
        try {
            page.locator("dwc-app-nav-item").first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(30000));
            System.out.println("[" + label + "] Dashboard ready. Title: '" + page.title() + "' | URL: " + page.url());
        } catch (Exception e) {
            System.err.println("[" + label + "] Warning: Dashboard locator wait timed out: " + e.getMessage());
        }
    }

    private static void navigateToView(Page page, String viewName, String label) {
        System.out.println("[" + label + "] Navigating to view '" + viewName + "'...");
        try {
            Locator navItem = page.locator("dwc-app-nav-item:has-text('" + viewName + "')").first();
            navItem.waitFor(new Locator.WaitForOptions().setTimeout(15000));
            navItem.click();
            sleep(500);
            System.out.println("[" + label + "] -> Entered '" + viewName + "'. Title: '" + page.title() + "' | URL: " + page.url());
        } catch (Exception e) {
            System.err.println("[" + label + "] Failed navigating to '" + viewName + "': " + e.getMessage());
        }
    }

    private static void performLogout(Page page, String label) {
        System.out.println("[" + label + "] Performing Logout...");
        try {
            Locator logoutBtn = page.locator("dwc-icon-button[name='logout'], [tooltip-text='Log out'], dwc-icon-button:has([name='logout'])").first();
            logoutBtn.waitFor(new Locator.WaitForOptions().setTimeout(15000));
            logoutBtn.click();
            System.out.println("[" + label + "] Clicked logout button. Waiting for login redirection...");

            try {
                page.locator("dwc-login").first().waitFor(new Locator.WaitForOptions().setTimeout(15000));
            } catch (Throwable ignored) {
            }
            System.out.println("[" + label + "] Logout complete. Current URL: " + page.url());
        } catch (Exception e) {
            System.err.println("[" + label + "] Logout notice: " + e.getMessage());
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
