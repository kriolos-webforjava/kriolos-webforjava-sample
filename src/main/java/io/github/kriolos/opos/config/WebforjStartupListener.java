package io.github.kriolos.opos.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import com.webforj.servlet.WebforjServlet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.annotation.WebListener;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;


@WebListener
@ApplicationScoped
public class WebforjStartupListener implements ServletContextListener {

    private static final Logger logger = Logger.getLogger(WebforjStartupListener.class);
    
    // Opcional: Gerar um nome único por reload previne colisões se o motor demorar a morrer
    private final String INTERNAL_SERVLET_NAME = "webforj-" + System.currentTimeMillis();

    // Guardamos a instância para destruí-la no Live Reload
    private WebforjServlet webforjServlet;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        org.eclipse.microprofile.config.Config mpConfig = ConfigProvider.getConfig();
        String mapping = mpConfig.getOptionalValue("webforj.servlet-mapping", String.class).orElse("/*");
        
        String entryClass = mpConfig.getOptionalValue("webforj.entry", String.class)
                .orElse("io.github.kriolos.opos.Application").trim();

        Config webforjConfig = buildTypesafeConfig(mpConfig);
        if (mapping.equals("/*") || mapping.equals("/")) {
            webforjConfig = webforjConfig.withValue("webforj.router.root", ConfigValueFactory.fromAnyRef("/"));
        }
        webforjConfig = webforjConfig.withValue("webforj.entry", ConfigValueFactory.fromAnyRef(entryClass));

        WebforjServlet.setConfig(webforjConfig);
        ServletContext context = sce.getServletContext();
        
        // Instancia e guarda na variável
        this.webforjServlet = new WebforjServlet();

        ServletRegistration.Dynamic registration = context.addServlet(INTERNAL_SERVLET_NAME, this.webforjServlet);
        if (registration != null) {
            registration.setLoadOnStartup(1);
            registration.addMapping(mapping);
            logger.infof("🚀 [webforJ] Servlet iniciado! (Nome interno: %s)", INTERNAL_SERVLET_NAME);
        }
    }

    private Config buildTypesafeConfig(org.eclipse.microprofile.config.Config mpConfig) {
        Config[] typesafeConfig = { ConfigFactory.empty() };
        for (String propertyName : mpConfig.getPropertyNames()) {
            if (propertyName.startsWith("webforj.")) {
                mpConfig.getOptionalValue(propertyName, String.class).ifPresent(value -> {
                    typesafeConfig[0] = typesafeConfig[0].withValue(
                            propertyName, 
                            ConfigValueFactory.fromAnyRef(value)
                    );
                });
            }
        }
        return typesafeConfig[0];
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (this.webforjServlet != null) {
            logger.warn("♻️ [Live Reload] Destruindo WebforjServlet para liberar o registro e a licença...");
            try {
                this.webforjServlet.destroy();
            } catch (Exception e) {
                logger.error("Falha ao desligar o Servlet do webforJ graciosamente", e);
            } finally {
                this.webforjServlet = null;
            }
        }
    }
}