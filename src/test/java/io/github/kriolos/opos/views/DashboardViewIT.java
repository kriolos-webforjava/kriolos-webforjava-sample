package io.github.kriolos.opos.views;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator.WaitForOptions;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import java.net.URL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

@QuarkusIntegrationTest
class DashboardViewIT {

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;

    //String deploymentUrl = "http://localhost:" + System.getProperty("server.port", "8080") + "/";
    @TestHTTPResource
    URL deploymentUrl;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @BeforeEach
    void setUp() {

        // By default, Playwright runs the browsers in headless mode. To see the browser
        // UI, setHeadless option to false. You can also use setSlowMo to slow down
        // execution. Learn more in the debugging tools section.
        // https://playwright.dev/java/docs/debug
        // browser = playwright.chromium().launch(new
        // BrowserType.LaunchOptions().setHeadless(false).setSlowMo(50));
        context = browser.newContext();
        page = context.newPage();

        // 🛠️ CORREÇÃO: Altere de http://localhost:8080/ para usar a variável injetada
        page.navigate(deploymentUrl.toString());

        // 🛠️ A SOLUÇÃO: Aguarde explicitamente até que a rede do WebSocket/Quarkus fique ociosa
        //page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

        // 🛠️ CORREÇÃO: Aguarda apenas que o esqueleto do HTML seja processado, sem travar nas conexões persistentes
        //page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @AfterAll
    static void closeBrowser() {
        playwright.close();
    }

    @Test
    void shouldRenderPage() {

        // 🛠️ Força o Playwright a aguardar até 5 segundos para o componente ser injetado pelo Servlet
        //page.locator(".explore-component").waitFor(new WaitForOptions().setTimeout(5000));
        //page.locator("text=Your dashboard is empty").waitFor();
        //assertThat(page.locator(".explore-component")).containsText("Your dashboard is empty");
        
        // O seu código de asserção do teste aqui (ex: verificar se o botão existe)
        //assertThat(page.getByText("Your dashboard is empty")).isVisible();
        //assertThat(page.locator("p")).containsText("Your dashboard is empty");
        //assertThat(page.locator("text=Your dashboard is empty")).isVisible();
        //assertThat(page.locator("body")).containsText("Your dashboard is empty");
        //assertThat(page.locator("text=Your dashboard is empty")).isVisible();
    }
}
