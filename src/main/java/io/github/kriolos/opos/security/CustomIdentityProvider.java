package io.github.kriolos.opos.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    /**
     * User account record stored in memory.
     */
    public record UserAccount(String username, String password, Set<String> roles) {
    }

    /**
     * In-memory user store containing 10 users and 5 distinct roles:
     * - admin
     * - manager
     * - supervisor
     * - cashier
     * - user
     */
    public static class InMemoryUserStore {
        private final Map<String, UserAccount> users = new ConcurrentHashMap<>();

        public InMemoryUserStore() {
            // 5 distinct roles: admin, manager, supervisor, cashier, user
            // 10 users:
            register("admin", "admin", Set.of("admin", "manager", "user"));
            register("manager", "manager", Set.of("manager", "supervisor", "user"));
            register("supervisor", "supervisor", Set.of("supervisor", "user"));
            register("cashier", "cashier", Set.of("cashier", "user"));
            register("user", "user", Set.of("user"));
            register("john", "john", Set.of("cashier", "user"));
            register("alice", "alice", Set.of("manager", "user"));
            register("bob", "bob", Set.of("supervisor", "user"));
            register("clara", "clara", Set.of("cashier", "user"));
            register("david", "david", Set.of("admin", "user"));
        }

        public void register(String username, String password, Set<String> roles) {
            users.put(username, new UserAccount(username, password, roles));
        }

        public Optional<UserAccount> findByUsername(String username) {
            return Optional.ofNullable(users.get(username));
        }

        public boolean validate(String username, String password) {
            UserAccount account = users.get(username);
            return account != null && account.password().equals(password);
        }

        public List<UserAccount> getAllUsers() {
            return new ArrayList<>(users.values());
        }
    }

    private final InMemoryUserStore userStore = new InMemoryUserStore();

    public InMemoryUserStore getUserStore() {
        return userStore;
    }

    @Override
    public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
        return UsernamePasswordAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest request,
            AuthenticationRequestContext context) {

        String username = request.getUsername();
        PasswordCredential passwordCredential = request.getPassword();
        String password = new String(passwordCredential.getPassword());

        if (username == null || password == null) {
            return Uni.createFrom().nullItem();
        }

        Optional<UserAccount> optUser = userStore.findByUsername(username);
        if (optUser.isPresent()) {
            UserAccount user = optUser.get();
            if (user.password().equals(password)) {
                QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder()
                        .setPrincipal(new QuarkusPrincipal(username))
                        .addCredential(passwordCredential);
                for (String role : user.roles()) {
                    builder.addRole(role);
                }
                return Uni.createFrom().item(builder.build());
            }
        }

        return Uni.createFrom().failure(new io.quarkus.security.AuthenticationFailedException("Invalid credentials"));
    }
}
