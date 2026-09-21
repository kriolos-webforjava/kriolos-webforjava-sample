package io.github.kriolos.opos.security;

import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CustomIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

    @Override
    public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
        return UsernamePasswordAuthenticationRequest.class;
    }


    @Override
    public Uni<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest request, AuthenticationRequestContext context) {
  
        String username = request.getUsername();
        PasswordCredential passwordCredential = request.getPassword();
        String password = new String(passwordCredential.getPassword());

        
        if (username == null || password == null) {
            return Uni.createFrom().nullItem();
        }

        if ("admin".equals(username) && "admin".equals(password)) {
            QuarkusSecurityIdentity identity = QuarkusSecurityIdentity.builder()
                    .setPrincipal(new QuarkusPrincipal(username))
                    .addRole("admin")
                    .addRole("user")
                    .addCredential(passwordCredential)
                    .build();
            return Uni.createFrom().item(identity);
        } 
        
        if ("user".equals(username) && "user".equals(password)) {
            QuarkusSecurityIdentity identity = QuarkusSecurityIdentity.builder()
                    .setPrincipal(new QuarkusPrincipal(username))
                    .addRole("user")
                    .addCredential(passwordCredential)
                    .build();
            return Uni.createFrom().item(identity);
        }
        
        return Uni.createFrom().failure(new io.quarkus.security.AuthenticationFailedException("Authenticaçao inválido"));
    }
}
