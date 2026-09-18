package io.github.kriolos.opos.views;

import java.util.Set;

import io.github.kriolos.opos.components.DrawerHeader;
import io.github.kriolos.opos.components.ThemeToggle;
import io.github.kriolos.opos.components.UserBadge;

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

  public MainLayout() {
    setHeader();
    setDrawer();
    setDrawerFooter();
    navigateRegistration = Router.getCurrent().onNavigate(this::onNavigate);
  }

  private void setHeader() {
    self.setDrawerHeaderVisible(true);
    self.addToDrawerTitle(new DrawerHeader());

    Toolbar toolbar = new Toolbar();
    toolbar.addToStart(new AppDrawerToggle(TablerIcon.create("layout-sidebar")));
    toolbar.addToTitle(title);
    toolbar.addToEnd(
        buildToolbarButton("search", "Search"),
        buildToolbarButton("bell", "Notifications"),
        new ThemeToggle(),
        new UserBadge("John Doe", "Admin"));

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
    self.addToDrawerFooter(buildToolbarButton("logout", "Logout"));
  }

  @Override
  protected void onDidDestroy() {
    if (navigateRegistration != null) {
      navigateRegistration.remove();
    }
  }

  private void onNavigate(NavigateEvent ev) {
    Set<Component> components = ev.getContext().getAllComponents();
    Component view = components.stream().filter(c -> c.getClass().getSimpleName().endsWith("View")).findFirst()
        .orElse(null);

    if (view != null) {
      FrameTitle frameTitle = view.getClass().getAnnotation(FrameTitle.class);
      title.setText(frameTitle != null ? frameTitle.value() : "");
    }
  }
}
