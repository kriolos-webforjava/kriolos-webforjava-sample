package io.github.kriolos.opos.views;

import java.util.Optional;

import com.webforj.component.Composite;
import com.webforj.component.html.elements.Div;
import com.webforj.component.login.Login;
import com.webforj.component.login.event.LoginSubmitEvent;
import com.webforj.component.toast.Toast;
import com.webforj.router.Router;
import com.webforj.router.annotation.FrameTitle;
import com.webforj.router.annotation.Route;
import com.webforj.router.event.DidEnterEvent;
import com.webforj.router.history.Location;
import com.webforj.router.history.ParametersBag;
import com.webforj.router.observer.DidEnterObserver;
import com.webforj.router.security.annotation.AnonymousAccess;

import io.quarkiverse.webforj.runtime.security.QuarkusRouteSecurityContext;
import io.quarkiverse.webforj.runtime.security.QuarkusRouteSecurityManager;
import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import jakarta.inject.Inject;

@Route("/login")
@FrameTitle("Login Submission")
@AnonymousAccess
public class LoginView extends Composite<Div> implements DidEnterObserver {

    private final Div self = getBoundComponent();
    private Login login = new Login();

    @Inject
    IdentityProviderManager identityProviderManager;

    @Inject
    QuarkusRouteSecurityContext securityContext;

    @Inject
    QuarkusRouteSecurityManager securityManager;

    public LoginView() {
        login.onSubmit(this::performLogin);
        self.add(login);
    }

    @Override
    public void onDidEnter(DidEnterEvent event, ParametersBag parameters) {
        if (securityContext != null && securityContext.isAuthenticated()) {
            navigateToMainPanel(null);
        } else {
            login.open();
        }
    }

    private void performLogin(LoginSubmitEvent ev) {
        String username = ev.getUsername();
        String password = ev.getPassword();

        // Create standard request expected by Quarkus IdentityProvider
        UsernamePasswordAuthenticationRequest authRequest = new UsernamePasswordAuthenticationRequest(
                username, new PasswordCredential(password.toCharArray())
        );

        // Perform reactive authentication
        identityProviderManager.authenticate(authRequest)
                .subscribe().with(
                        securityIdentity -> {
                            // 1. SUCCESS: Close login dialog
                            login.close();
                            login.setError(false).setEnabled(false);

                            // 2. Bind authenticated identity to webforJ session
                            securityContext.setSecurityIdentity(securityIdentity);

                            Toast.show("Welcome, " + securityIdentity.getPrincipal().getName() + "!");

                            // 3. Redirect to destination or dashboard
                            navigateToMainPanel(securityIdentity);
                        },
                        failure -> {
                            login.setError(true).setEnabled(true);
                            // ERROR: Invalid credentials
                            Toast.show("Error: " + failure.getMessage());
                        }
                );
    }

    private void navigateToMainPanel(SecurityIdentity identity) {
        login.close();
        Optional<Location> preAuth = securityManager.getPreAuthenticationLocation();
        if (preAuth.isPresent()) {
            securityManager.clearPreAuthenticationLocation();
            Router.getCurrent().navigate(preAuth.get());
        } else {
            Router.getCurrent().navigate(DashboardView.class);
        }
    }
}
