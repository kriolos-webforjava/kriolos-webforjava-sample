package io.github.kriolos.opos;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;

public class MultiBrowserFirefoxTest {

    public static void main(String[] args) {
        String baseUrl = "http://localhost:8080";
        System.out.println("=== Starting Multi-Browser Firefox Test ===");
        System.out.println("Target URL: " + baseUrl);

        try (Playwright playwright = Playwright.create()) {
            System.out.println("Launching Playwright Firefox...");
            Browser browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(false));
            System.out.println("Firefox launched successfully!");

            Page.NavigateOptions navOptions = new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(60000);

            // =========================================================================
            // 1) Open first "browser" and try navigate between pages (side-menu)
            // =========================================================================
            System.out.println("\n--- [Step 1] Opening Browser 1 ---");
            BrowserContext context1 = browser.newContext();
            Page page1 = context1.newPage();
            setupConsoleLogging(page1, "Browser 1");

            System.out.println("Browser 1 navigating to " + baseUrl);
            page1.navigate(baseUrl, navOptions);

            System.out.println("Browser 1 waiting for 'Your dashboard is empty'...");
            page1.locator("text=Your dashboard is empty").waitFor();
            System.out.println("Browser 1 loaded. Title: " + page1.title());
            System.out.println("Browser 1 URL: " + page1.url());

            // Navigate to Contacts
            System.out.println("Browser 1 clicking 'Contacts' in side-menu...");
            page1.locator("dwc-app-nav-item:has-text('Contacts')").click();
            Thread.sleep(1500);
            System.out.println("Browser 1 after clicking Contacts. Title: " + page1.title() + " | URL: " + page1.url());

            // Navigate to Deals
            System.out.println("Browser 1 clicking 'Deals' in side-menu...");
            page1.locator("dwc-app-nav-item:has-text('Deals')").click();
            Thread.sleep(1500);
            System.out.println("Browser 1 after clicking Deals. Title: " + page1.title() + " | URL: " + page1.url());


            // =========================================================================
            // 2) Open a second "browser" and try navigate between pages (side-menu)
            // =========================================================================
            System.out.println("\n--- [Step 2] Opening Browser 2 ---");
            BrowserContext context2 = browser.newContext();
            Page page2 = context2.newPage();
            setupConsoleLogging(page2, "Browser 2");

            System.out.println("Browser 2 navigating to " + baseUrl);
            page2.navigate(baseUrl, navOptions);

            System.out.println("Browser 2 waiting for 'Your dashboard is empty'...");
            page2.locator("text=Your dashboard is empty").waitFor();
            System.out.println("Browser 2 loaded. Title: " + page2.title());
            System.out.println("Browser 2 URL: " + page2.url());

            // Navigate to Tasks
            System.out.println("Browser 2 clicking 'Tasks' in side-menu...");
            page2.locator("dwc-app-nav-item:has-text('Tasks')").click();
            Thread.sleep(1500);
            System.out.println("Browser 2 after clicking Tasks. Title: " + page2.title() + " | URL: " + page2.url());

            // Navigate to Calendar
            System.out.println("Browser 2 clicking 'Calendar' in side-menu...");
            page2.locator("dwc-app-nav-item:has-text('Calendar')").click();
            Thread.sleep(1500);
            System.out.println("Browser 2 after clicking Calendar. Title: " + page2.title() + " | URL: " + page2.url());


            // =========================================================================
            // 3) Open a third "browser" and try navigate between pages (side-menu)
            // =========================================================================
            System.out.println("\n--- [Step 3] Opening Browser 3 ---");
            BrowserContext context3 = browser.newContext();
            Page page3 = context3.newPage();
            setupConsoleLogging(page3, "Browser 3");

            System.out.println("Browser 3 navigating to " + baseUrl);
            page3.navigate(baseUrl, navOptions);

            System.out.println("Browser 3 waiting for 'Your dashboard is empty'...");
            page3.locator("text=Your dashboard is empty").waitFor();
            System.out.println("Browser 3 loaded. Title: " + page3.title());
            System.out.println("Browser 3 URL: " + page3.url());

            // Navigate to Reports
            System.out.println("Browser 3 clicking 'Reports' in side-menu...");
            page3.locator("dwc-app-nav-item:has-text('Reports')").click();
            Thread.sleep(1500);
            System.out.println("Browser 3 after clicking Reports. Title: " + page3.title() + " | URL: " + page3.url());


            // =========================================================================
            // 4) Close first browser and verify Browser 2 & Browser 3 continue working
            // =========================================================================
            System.out.println("\n--- [Step 4] Closing Browser 1 ---");
            context1.close();
            System.out.println("Browser 1 closed successfully.");

            System.out.println("\n--- Verifying Browser 2 & 3 still active and responsive ---");
            System.out.println("Browser 2 navigating back to 'Dashboard'...");
            page2.locator("dwc-app-nav-item:has-text('Dashboard')").click();
            Thread.sleep(1500);
            System.out.println("Browser 2 after clicking Dashboard. Title: " + page2.title() + " | URL: " + page2.url());

            System.out.println("Browser 3 navigating to 'Tasks'...");
            page3.locator("dwc-app-nav-item:has-text('Tasks')").click();
            Thread.sleep(1500);
            System.out.println("Browser 3 after clicking Tasks. Title: " + page3.title() + " | URL: " + page3.url());

            context2.close();
            context3.close();
            browser.close();

            System.out.println("\n=== All Steps (1, 2, 3, 4) Completed Successfully! ===");
        } catch (Exception e) {
            System.err.println("Test failed with exception: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
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
