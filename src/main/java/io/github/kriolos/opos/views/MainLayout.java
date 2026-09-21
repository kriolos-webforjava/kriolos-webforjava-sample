package io.github.kriolos.opos.views;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import io.github.kriolos.opos.components.DrawerHeader;
import io.github.kriolos.opos.components.ThemeToggle;
import io.github.kriolos.opos.components.UserBadge;
import io.quarkiverse.webforj.runtime.security.QuarkusRouteSecurityContext;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.security.identity.SecurityIdentity;

import com.webforj.component.Component;
import com.webforj.component.Composite;
import com.webforj.component.Theme;
import com.webforj.component.html.elements.H1;
import com.webforj.component.icons.IconButton;
import com.webforj.component.icons.TablerIcon;
import com.webforj.component.layout.applayout.AppDrawerToggle;
import com.webforj.component.layout.applayout.AppLayout;
import com.webforj.component.layout.appnav.AppNav;
import com.webforj.component.layout.appnav.AppNavItem;
import com.webforj.component.layout.toolbar.Toolbar;
import com.webforj.component.toast.Toast;
import com.webforj.dispatcher.ListenerRegistration;
import com.webforj.router.Router;
import com.webforj.router.annotation.FrameTitle;
import com.webforj.router.annotation.Route;
import com.webforj.router.event.NavigateEvent;

@Route
public class MainLayout extends Composite<AppLayout> {
  private AppLayout self = getBoundComponent();
  private H1 title = new H1();
  private ListenerRegistration<NavigateEvent> navigateRegistration;
  private UserBadge userBadge;

  @Inject
  QuarkusRouteSecurityContext securityContext;

  public MainLayout() {
    this(resolveSecurityContext());
  }

  @Inject
  public MainLayout(QuarkusRouteSecurityContext securityContext) {
    this.securityContext = securityContext;
    setHeader();
    setDrawer();
    setDrawerFooter();
    navigateRegistration = Router.getCurrent().onNavigate(this::onNavigate);
  }

  private static QuarkusRouteSecurityContext resolveSecurityContext() {
    try {
      if (Arc.container() != null) {
        InstanceHandle<QuarkusRouteSecurityContext> handle =
            Arc.container().instance(QuarkusRouteSecurityContext.class);
        if (handle.isAvailable()) {
          return handle.get();
        }
      }
    } catch (Throwable ignored) {
    }
    return null;
  }

  @PostConstruct
  public void onInit() {
    updateUserInfo();
  }

  public void updateUserInfo() {
    if (userBadge != null) {
      userBadge.setUser(resolveUserName(), resolveUserRole());
    }
  }

  private String resolveUserName() {
    if (securityContext != null) {
      SecurityIdentity identity = securityContext.getSecurityIdentity();
      if (identity != null && !identity.isAnonymous() && identity.getPrincipal() != null) {
        String name = identity.getPrincipal().getName();
        if (name != null && !name.isBlank()) {
          return capitalize(name);
        }
      }
    }
    return "Convidado";
  }

  private String resolveUserRole() {
    if (securityContext != null) {
      SecurityIdentity identity = securityContext.getSecurityIdentity();
      if (identity != null && !identity.isAnonymous()) {
        Set<String> roles = identity.getRoles();
        if (roles != null && !roles.isEmpty()) {
          return roles.stream()
              .map(this::capitalize)
              .collect(Collectors.joining(", "));
        }
        return "Utilizador";
      }
    }
    return "";
  }

  private String capitalize(String str) {
    if (str == null || str.isBlank()) return str;
    if (str.startsWith("ROLE_")) str = str.substring(5);
    return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
  }

  private void performLogout() {
    if (securityContext != null) {
      securityContext.clearSecurityIdentity();
    }
    Toast.show("Sessão terminada com sucesso!", 3000, Theme.SUCCESS, Toast.Placement.BOTTOM_RIGHT);
    Router.getCurrent().navigate(LoginView.class);
  }

  private void setHeader() {
    self.setDrawerHeaderVisible(true);
    self.addToDrawerTitle(new DrawerHeader());

    Toolbar toolbar = new Toolbar();
    toolbar.addToStart(new AppDrawerToggle(TablerIcon.create("layout-sidebar")));
    toolbar.addToTitle(title);

    userBadge = new UserBadge(resolveUserName(), resolveUserRole());

    IconButton logoutHeaderBtn = new IconButton(TablerIcon.create("logout"));
    logoutHeaderBtn.setTooltipText("Terminar Sessão");
    logoutHeaderBtn.onClick(ev -> performLogout());

    toolbar.addToEnd(
        buildToolbarButton("search", "Search"),
        buildToolbarButton("bell", "Notifications"),
        new ThemeToggle(),
        userBadge,
        logoutHeaderBtn);

    self.addToHeader(toolbar);
  }

  private IconButton buildToolbarButton(String iconName, String label) {
    IconButton button = new IconButton(TablerIcon.create(iconName));
    button.onClick(ev -> Toast.show("\"%s\" is not wired up yet".formatted(label), 3000, Theme.INFO,
        Toast.Placement.BOTTOM_RIGHT));
    return button;
  }

  private void setDrawer() {
    AppNav appNav = new AppNav();
    appNav.addItem(new AppNavItem("Dashboard", DashboardView.class, TablerIcon.create("layout-dashboard")));
    appNav.addItem(new AppNavItem("Contacts", ContactsView.class, TablerIcon.create("users")));
    appNav.addItem(new AppNavItem("Deals", DealsView.class, TablerIcon.create("briefcase")));
    appNav.addItem(new AppNavItem("Tasks", TasksView.class, TablerIcon.create("checklist")));
    appNav.addItem(new AppNavItem("Calendar", CalendarView.class, TablerIcon.create("calendar-event")));
    appNav.addItem(new AppNavItem("Reports", ReportsView.class, TablerIcon.create("chart-bar")));

    self.addToDrawer(appNav);
  }

  private void setDrawerFooter() {
    self.setDrawerFooterVisible(true);
    IconButton logoutBtn = new IconButton(TablerIcon.create("logout"));
    logoutBtn.setTooltipText("Terminar Sessão");
    logoutBtn.onClick(ev -> performLogout());
    self.addToDrawerFooter(logoutBtn);
  }

  @Override
  protected void onDidDestroy() {
    if (navigateRegistration != null) {
      navigateRegistration.remove();
    }
  }

  private void onNavigate(NavigateEvent ev) {
    updateUserInfo();
    Set<Component> components = ev.getContext().getAllComponents();
    Component view = components.stream().filter(c -> c.getClass().getSimpleName().endsWith("View")).findFirst()
        .orElse(null);

    if (view != null) {
      FrameTitle frameTitle = view.getClass().getAnnotation(FrameTitle.class);
      title.setText(frameTitle != null ? frameTitle.value() : "");
    }
  }
}
