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
        login.onSubmit(this::realizarLogin);
        self.add(login);
    }

    @Override
    public void onDidEnter(DidEnterEvent event, ParametersBag parameters) {
        if (securityContext != null && securityContext.isAuthenticated()) {
            exibirPainelPrincipal(null);
        } else {
            login.open();
        }
    }

    private void realizarLogin(LoginSubmitEvent ev) {
        String username = ev.getUsername();
        String password = ev.getPassword();

        // Cria a requisição padrão que o nosso IdentityProvider espera
        UsernamePasswordAuthenticationRequest authRequest = new UsernamePasswordAuthenticationRequest(
                username, new PasswordCredential(password.toCharArray())
        );

        // Executa a autenticação de forma reativa/assíncrona
        identityProviderManager.authenticate(authRequest)
                .subscribe().with(
                        securityIdentity -> {
                            // 1. SUCESSO: Fecha o diálogo de login
                            login.close();
                            login.setError(false).setEnabled(false);

                            // 2. Armazena a identidade autenticada na sessão do webforJ
                            securityContext.setSecurityIdentity(securityIdentity);

                            Toast.show("Bem-vindo, " + securityIdentity.getPrincipal().getName() + "!");

                            // 3. Redireciona para o painel principal
                            exibirPainelPrincipal(securityIdentity);
                        },
                        failure -> {
                            login.setError(true).setEnabled(true);
                            // ERRO: Credenciais erradas
                            Toast.show("Erro: " + failure.getMessage());
                        }
                );
    }

    private void exibirPainelPrincipal(SecurityIdentity identity) {
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
