package io.github.kriolos.opos.components;

import com.webforj.component.Composite;
import com.webforj.component.Theme;
import com.webforj.component.button.Button;
import com.webforj.component.button.ButtonTheme;
import com.webforj.component.html.elements.Paragraph;
import com.webforj.component.icons.Icon;
import com.webforj.component.icons.TablerIcon;
import com.webforj.component.layout.flexlayout.FlexAlignment;
import com.webforj.component.layout.flexlayout.FlexDirection;
import com.webforj.component.layout.flexlayout.FlexJustifyContent;
import com.webforj.component.layout.flexlayout.FlexLayout;
import com.webforj.component.toast.Toast;

public class Explore extends Composite<FlexLayout> {
  private FlexLayout self = getBoundComponent();

  public Explore(String message, String iconName, String ctaLabel) {
    self.addClassName("explore-component");
    self.setStyle("margin", "1em auto");
    self.setHeight("100%");
    self.setDirection(FlexDirection.COLUMN);
    self.setAlignment(FlexAlignment.CENTER);
    self.setJustifyContent(FlexJustifyContent.CENTER);
    self.setMaxWidth(300);
    self.setSpacing(".75em");

    Icon icon = TablerIcon.create(iconName);
    icon.setStyle("font-size", "3rem");

    FlexLayout badge = FlexLayout.create(icon).align().center().justify().center().build();
    badge.setStyle("width", "6rem");
    badge.setStyle("height", "6rem");
    badge.setStyle("border-radius", "50%");
    badge.setStyle("background", "var(--dwc-color-primary-alt)");
    badge.setStyle("color", "var(--dwc-color-on-primary-text-alt)");

    Paragraph messageLabel = new Paragraph(message);
    messageLabel.setStyle("color", "var(--dwc-color-gray-text-light)");
    messageLabel.setStyle("margin", "0");

    Button cta = new Button(ctaLabel)
        .setPrefixComponent(TablerIcon.create("plus"))
        .setTheme(ButtonTheme.PRIMARY);
    cta.onClick(ev -> Toast.show("\"%s\" is not wired up yet".formatted(ctaLabel), 3000, Theme.INFO,
        Toast.Placement.BOTTOM_RIGHT));

    self.add(badge, messageLabel, cta);
  }
}
