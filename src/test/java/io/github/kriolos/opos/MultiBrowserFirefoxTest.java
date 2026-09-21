package io.github.kriolos.opos;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import java.util.ArrayList;
import java.util.List;

public class MultiBrowserFirefoxTest {

    private static String baseUrl = "http://localhost:8080";
    private static Page.NavigateOptions navOptions = new Page.NavigateOptions()
            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            .setTimeout(60000);

    public static void main(String[] args) {
        System.out.println("=== Starting Multi-Browser Firefox Test ===");
        System.out.println("Target URL: " + baseUrl);

        try {
            test1();
        } catch (Exception ex) {
            System.getLogger(MultiBrowserFirefoxTest.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);

            System.exit(1);
        }
    }

    private static void test1() {
        System.out.println("Starting tasks...");
        List<Thread> threads = new ArrayList<>();

        try {
            // 1. Create and start 100 virtual threads
            for (int i = 1; i <= 1; i++) {
                final int taskId = i;
                Thread vThread = Thread.ofVirtual().start(() -> {

                    String name = "TEST#" + taskId;

                    Thread.currentThread().setName(name);

                    System.out.println(name + " START.");

                    System.out.println(name + " TEST Launching ...");
                    try ( Playwright playwright = Playwright.create();Browser browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(true)); BrowserContext context1 = browser.newContext();) {

                        openBrowserNavigate(context1, name);

                        System.out.println(name + " TEST Finished successfully!");
                    } catch (Exception e) {
                        System.err.println(name + " TEST failed with exception: ");
                        //e.printStackTrace();
                    }
                });
                threads.add(vThread);
            }
        } catch (Exception e) {
            System.err.println(" Playwright failed with exception: " + e.getMessage());
            //e.printStackTrace();
        }

        // 2. Wait (join) for all of them to finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                System.getLogger(MultiBrowserFirefoxTest.class.getName()).log(System.Logger.Level.ERROR, "Exception on join", ex);
            }
        }

        System.out.println("All 100 virtual threads have finished!");
    }

    private static void waitT(int tempo) {

        try {
            Thread.sleep(tempo);
        } catch (InterruptedException ex) {
            System.getLogger(MultiBrowserFirefoxTest.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    private static void openBrowserNavigate(BrowserContext context1, String name) {

        System.out.println(name + "[Step 1] Opening ");
        Page page = context1.newPage();
        setupConsoleLogging(page, name);

        System.out.println(name + " navigating to " + baseUrl);
        page.navigate(baseUrl, navOptions);

        System.out.println(name + " waiting for 'Your dashboard is empty'...");
        page.locator("text=Your dashboard is empty").waitFor();
        System.out.println(name + " loaded. Title: " + page.title());
        System.out.println(name + " URL: " + page.url());

        // Navigate to Contacts
        System.out.println(name + " clicking 'Contacts' in side-menu...");
        page.locator("dwc-app-nav-item:has-text('Contacts')").click();

        System.out.println(name + " after clicking Contacts. Title: " + page.title() + " | URL: " + page.url());

        // Navigate to Deals
        System.out.println(name + " clicking 'Deals' in side-menu...");
        page.locator("dwc-app-nav-item:has-text('Deals')").click();
        waitT(1500);
        System.out.println(name + " after clicking Deals. Title: " + page.title() + " | URL: " + page.url());

        // Navigate to Tasks
        System.out.println(name + " clicking 'Tasks' in side-menu...");
        page.locator("dwc-app-nav-item:has-text('Tasks')").click();
        waitT(1500);
        System.out.println(name + " after clicking Tasks. Title: " + page.title() + " | URL: " + page.url());

        // Navigate to Calendar
        System.out.println(name + " clicking 'Calendar' in side-menu...");
        page.locator("dwc-app-nav-item:has-text('Calendar')").click();
        waitT(1500);
        System.out.println(name + " after clicking Calendar. Title: " + page.title() + " | URL: " + page.url());

        // Navigate to Reports
        System.out.println(name + " clicking 'Reports' in side-menu...");
        page.locator("dwc-app-nav-item:has-text('Reports')").click();
        waitT(1500);
        System.out.println(name + " after clicking Reports. Title: " + page.title() + " | URL: " + page.url());

        System.out.println(name + " navigating to 'Tasks'...");
        page.locator("dwc-app-nav-item:has-text('Tasks')").click();
        waitT(1500);
        System.out.println(name + " after clicking Tasks. Title: " + page.title() + " | URL: " + page.url());

        waitT(60000);
        System.out.println(name + "[Step END] Closing Browser---");
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
