package io.github.kriolos.opos.config;

import com.webforj.servlet.WebforjServlet;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

/**
 * Destroys the {@link WebforjServlet} when the Quarkus application context closes.
 *
 * <p>
 * The servlet cleanup releases Basis licensing and terminates the runtime threads.
 * Closing the context is the only signal that fires on every shutdown, including
 * Quarkus Live Coding restarts. The listener runs at the lowest precedence
 * (highest Priority value) to ensure that other listeners run first.
 * </p>
 */
@ApplicationScoped
public class WebforjServletShutdownListener {

    private static final Logger logger = Logger.getLogger(WebforjServletShutdownListener.class);

    private WebforjServlet webforjServlet;

    /**
     * Stores the servlet instance registered during startup.
     *
     * @param webforjServlet the webforJ servlet instance
     */
    public void registerServlet(WebforjServlet webforjServlet) {
        this.webforjServlet = webforjServlet;
    }

    /**
     * Observes the Quarkus shutdown event.
     * The value 5000 ensures it runs at the end of the queue (Lowest Precedence).
     */
    void onApplicationEvent(@Observes @Priority(5000) ShutdownEvent event) {
        if (this.webforjServlet != null) {
            logger.debug("Destroying WebforjServlet on context close to release licensing and threads");
            this.webforjServlet.destroy();
        }
    }
}