package io.github.kriolos.opos.views;

import com.webforj.Page;
import com.webforj.component.Composite;
import com.webforj.component.button.Button;
import com.webforj.component.html.elements.Div;
import com.webforj.component.login.Login;
import com.webforj.component.login.event.LoginSubmitEvent;
import com.webforj.component.toast.Toast;
import com.webforj.dispatcher.EventListener;
import com.webforj.router.Router;
import com.webforj.router.annotation.FrameTitle;
import com.webforj.router.annotation.Route;
import com.webforj.router.security.annotation.AnonymousAccess;
import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import jakarta.inject.Inject;

@Route("/login")
@FrameTitle("Login Submission")
@AnonymousAccess
public class LoginView extends Composite<Div> {

    private final Div self = getBoundComponent();
    private Login login = new Login();

    @Inject
    IdentityProviderManager identityProviderManager;

    //@Inject
    //Router router = Router.getCurrent();

    public LoginView() {

        login.onSubmit(this::realizarLogin);

        login.open();
        self.add(login);
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
                            // SUCESSO: Armazena a identidade na sessão e redireciona
                            login.setError(false).setEnabled(false);
                            Toast.show("Bem-vindo, " + securityIdentity.getPrincipal().getName() + "!");
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
        Router.getCurrent().navigate(DashboardView.class);
    }
}
