package io.github.kriolos.opos.components;

import com.webforj.component.Composite;
import com.webforj.component.avatar.Avatar;
import com.webforj.component.html.elements.Span;
import com.webforj.component.layout.flexlayout.FlexAlignment;
import com.webforj.component.layout.flexlayout.FlexLayout;

public class UserBadge extends Composite<FlexLayout> {
  private FlexLayout self = getBoundComponent();

  public UserBadge(String name, String role) {
    self.setAlignment(FlexAlignment.CENTER);
    self.setSpacing(".5em");
    self.setStyle("padding-left", "var(--dwc-space-m)");
    self.setStyle("background-image",
        "linear-gradient(to bottom, transparent, var(--dwc-border-color) 20%, var(--dwc-border-color) 80%, transparent)");
    self.setStyle("background-repeat", "no-repeat");
    self.setStyle("background-position", "left");
    self.setStyle("background-size", "1px 100%");

    Span nameLabel = new Span(name);
    nameLabel.setStyle("font-size", "var(--dwc-font-size-s)");
    nameLabel.setStyle("font-weight", "var(--dwc-font-weight-medium)");
    nameLabel.setStyle("line-height", "1.2");

    Span roleLabel = new Span(role);
    roleLabel.setStyle("font-size", "var(--dwc-font-size-xs)");
    roleLabel.setStyle("color", "var(--dwc-color-gray-text-light)");
    roleLabel.setStyle("line-height", "1.2");

    FlexLayout details = FlexLayout.create(nameLabel, roleLabel).vertical().build();
    details.setSpacing("0");

    self.add(details, new Avatar(name));
  }
}
