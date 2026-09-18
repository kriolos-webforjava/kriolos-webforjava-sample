package io.github.kriolos.opos.components;

import com.webforj.App;
import com.webforj.component.Composite;
import com.webforj.component.html.elements.Div;
import com.webforj.component.icons.IconButton;
import com.webforj.component.icons.TablerIcon;

public class ThemeToggle extends Composite<Div> {
  private final Div self = getBoundComponent();
  private final IconButton button = new IconButton(TablerIcon.create(iconForTheme(App.getTheme())));

  public ThemeToggle() {
    button.onClick(ev -> {
      String next = "dark".equals(App.getTheme()) ? "light" : "dark";
      App.setTheme(next);
      button.setName(iconForTheme(next));
    });
    self.add(button);
  }

  private static String iconForTheme(String theme) {
    return "dark".equals(theme) ? "sun" : "moon";
  }
}
