package io.github.kriolos.opos;

import java.util.ArrayList;
import java.util.List;

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
 * Configurable properties (via constants or JVM system properties):
 * <ul>
 *   <li>{@code test.base.url} - Target URL (default: http://localhost:8080)</li>
 *   <li>{@code test.browser.count} - Number of concurrent browser threads (default: 1)</li>
 *   <li>{@code test.headless} - Run headless or visual (default: true)</li>
 *   <li>{@code test.login.enabled} - Whether to perform login (default: true)</li>
 *   <li>{@code test.username} - Login username (default: admin)</li>
 *   <li>{@code test.password} - Login password (default: senhaSuperSegura123)</li>
 *   <li>{@code test.view.stay.time.ms} - Dwell time per view in ms (default: 1500)</li>
 *   <li>{@code test.logout.enabled} - Whether to perform logout at the end (default: true)</li>
 *   <li>{@code test.post.dwell.ms} - Dwell time before logout/finish in ms (default: 3000)</li>
 * </ul>
 * </p>
 */
public class MultiBrowserFirefoxTest {

    // =========================================================================
    // CONFIGURATION (Customize directly or override via -Dproperty=value)
    // =========================================================================
    public static final String BASE_URL = System.getProperty("test.base.url", "http://localhost:8080");
    public static final int NUM_BROWSERS = Integer.getInteger("test.browser.count", 1);
    public static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("test.headless", "true"));

    // Security & Login
    public static final boolean ENABLE_LOGIN = Boolean.parseBoolean(System.getProperty("test.login.enabled", "true"));
    public static final String USERNAME = System.getProperty("test.username", "admin");
    public static final String PASSWORD = System.getProperty("test.password", "senhaSuperSegura123");

    // Timing (easily adjustable)
    public static final int VIEW_STAY_TIME_MS = Integer.getInteger("test.view.stay.time.ms", 1500);
    public static final int POST_NAVIGATION_DWELL_MS = Integer.getInteger("test.post.dwell.ms", 3000);
    public static final boolean ENABLE_LOGOUT = Boolean.parseBoolean(System.getProperty("test.logout.enabled", "true"));

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
        System.out.println("Login check:        " + (ENABLE_LOGIN ? "ENABLED (" + USERNAME + ")" : "DISABLED"));
        System.out.println("View dwell time:    " + VIEW_STAY_TIME_MS + " ms");
        System.out.println("Logout enabled:     " + ENABLE_LOGOUT);
        System.out.println("Views to visit:     " + String.join(", ", ALL_VIEWS));
        System.out.println("=================================================");

        try {
            runConcurrentBrowserTests();
        } catch (Exception ex) {
            System.getLogger(MultiBrowserFirefoxTest.class.getName())
                    .log(System.Logger.Level.ERROR, "Fatal test error", ex);
            System.exit(1);
        }
    }

    private static void runConcurrentBrowserTests() {
        List<Thread> threads = new ArrayList<>();

        for (int i = 1; i <= NUM_BROWSERS; i++) {
            final int taskId = i;
            Thread vThread = Thread.ofVirtual().start(() -> {
                String label = "TEST#" + taskId;
                Thread.currentThread().setName(label);
                System.out.println("[" + label + "] STARTING...");

                BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                        .setHeadless(HEADLESS);

                try (Playwright playwright = Playwright.create();
                     Browser browser = playwright.firefox().launch(launchOptions);
                     BrowserContext context = browser.newContext()) {

                    executeBrowserSession(context, label);
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

        System.out.println("=== All " + NUM_BROWSERS + " browser session(s) completed ===");
    }

    private static void executeBrowserSession(BrowserContext context, String label) {
        Page page = context.newPage();
        setupConsoleLogging(page, label);

        // 1. Navigate to base URL
        System.out.println("[" + label + "] Navigating to " + BASE_URL);
        page.navigate(BASE_URL, NAV_OPTIONS);

        // 2. Perform Login if enabled/required
        if (ENABLE_LOGIN) {
            performLoginIfRequired(page, label);
        }

        // 3. Ensure Dashboard is loaded
        waitForDashboard(page, label);

        // 4. Navigate through all views
        for (String viewName : ALL_VIEWS) {
            navigateToView(page, viewName, label);
            sleep(VIEW_STAY_TIME_MS);
        }

        // 5. Dwell time before logout
        if (POST_NAVIGATION_DWELL_MS > 0) {
            System.out.println("[" + label + "] Waiting " + POST_NAVIGATION_DWELL_MS + " ms before logout...");
            sleep(POST_NAVIGATION_DWELL_MS);
        }

        // 6. Perform Logout if enabled
        if (ENABLE_LOGOUT) {
            performLogout(page, label);
        }

        System.out.println("[" + label + "] Closing browser session.");
    }

    private static void performLoginIfRequired(Page page, String label) {
        // Detect if login is required by checking URL or presence of dwc-login
        boolean onLoginPage = page.url().contains("/login");
        Locator loginLocator = page.locator("dwc-login");

        // Wait up to 3s to see if dwc-login appears or redirect to /login occurs
        try {
            loginLocator.waitFor(new Locator.WaitForOptions().setTimeout(3000));
            onLoginPage = true;
        } catch (Throwable ignored) {
        }

        if (onLoginPage || loginLocator.isVisible()) {
            System.out.println("[" + label + "] Login required. Authenticating as '" + USERNAME + "'...");

            // Fill username
            Locator usernameField = page.locator("dwc-login input:not([type='password']), input[type='text']").first();
            usernameField.fill(USERNAME);

            // Fill password
            Locator passwordField = page.locator("dwc-login input[type='password']").first();
            passwordField.fill(PASSWORD);

            // Submit login
            Locator submitButton = page.locator("dwc-login button, dwc-button:has-text('Sign in'), button:has-text('Sign in')").first();
            if (submitButton.isVisible()) {
                submitButton.click();
            } else {
                passwordField.press("Enter");
            }

            System.out.println("[" + label + "] Submitted credentials. Waiting for authentication...");
            sleep(1500);
        } else {
            System.out.println("[" + label + "] No login dialog detected (already authenticated or login not enforced).");
        }
    }

    private static void waitForDashboard(Page page, String label) {
        System.out.println("[" + label + "] Waiting for dashboard to load...");
        try {
            page.locator("text=Your dashboard is empty, dwc-app-nav-item").first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(15000));
            System.out.println("[" + label + "] Dashboard ready. Title: '" + page.title() + "' | URL: " + page.url());
        } catch (Exception e) {
            System.out.println("[" + label + "] Warning: Dashboard locator wait timed out, continuing: " + e.getMessage());
        }
    }

    private static void navigateToView(Page page, String viewName, String label) {
        System.out.println("[" + label + "] Navigating to view '" + viewName + "'...");
        Locator navItem = page.locator("dwc-app-nav-item:has-text('" + viewName + "')").first();
        try {
            navItem.waitFor(new Locator.WaitForOptions().setTimeout(10000));
            navItem.click();
            System.out.println("[" + label + "] -> Entered '" + viewName + "'. Title: '" + page.title() + "' | URL: " + page.url());
        } catch (Exception e) {
            System.err.println("[" + label + "] Failed navigating to '" + viewName + "': " + e.getMessage());
        }
    }

    private static void performLogout(Page page, String label) {
        System.out.println("[" + label + "] Performing Logout...");
        try {
            // Find logout button (either in top toolbar header or drawer footer)
            Locator logoutBtn = page.locator("dwc-icon-button[name='logout'], [tooltip-text='Log out'], dwc-icon-button:has([name='logout'])").first();
            logoutBtn.waitFor(new Locator.WaitForOptions().setTimeout(5000));
            logoutBtn.click();
            System.out.println("[" + label + "] Clicked logout button.");

            // Wait for toast or redirect to /login
            sleep(1500);
            System.out.println("[" + label + "] After logout -> Title: '" + page.title() + "' | URL: " + page.url());
        } catch (Exception e) {
            System.err.println("[" + label + "] Logout button not found or click failed: " + e.getMessage());
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
