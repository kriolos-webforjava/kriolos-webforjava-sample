package io.github.kriolos.opos.components;

import com.webforj.component.Composite;
import com.webforj.component.avatar.Avatar;
import com.webforj.component.html.elements.Span;
import com.webforj.component.layout.flexlayout.FlexAlignment;
import com.webforj.component.layout.flexlayout.FlexLayout;

public class UserBadge extends Composite<FlexLayout> {
  private FlexLayout self = getBoundComponent();
  private Span nameLabel = new Span();
  private Span roleLabel = new Span();
  private Avatar avatar = new Avatar();

  public UserBadge() {
    this("Guest", "");
  }

  public UserBadge(String name, String role) {
    self.setAlignment(FlexAlignment.CENTER);
    self.setSpacing(".5em");
    self.setStyle("padding-left", "var(--dwc-space-m)");
    self.setStyle("background-image",
        "linear-gradient(to bottom, transparent, var(--dwc-border-color) 20%, var(--dwc-border-color) 80%, transparent)");
    self.setStyle("background-repeat", "no-repeat");
    self.setStyle("background-position", "left");
    self.setStyle("background-size", "1px 100%");

    nameLabel.setStyle("font-size", "var(--dwc-font-size-s)");
    nameLabel.setStyle("font-weight", "var(--dwc-font-weight-medium)");
    nameLabel.setStyle("line-height", "1.2");

    roleLabel.setStyle("font-size", "var(--dwc-font-size-xs)");
    roleLabel.setStyle("color", "var(--dwc-color-gray-text-light)");
    roleLabel.setStyle("line-height", "1.2");

    FlexLayout details = FlexLayout.create(nameLabel, roleLabel).vertical().build();
    details.setSpacing("0");

    self.add(details, avatar);
    setUser(name, role);
  }

  public void setUser(String name, String role) {
    nameLabel.setText(name != null ? name : "");
    roleLabel.setText(role != null ? role : "");
    if (name != null && !name.isBlank()) {
      avatar.setText(name);
      avatar.setLabel(name);
      avatar.setInitials(name.length() > 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase());
    }
  }

  public String getName() {
    return nameLabel.getText();
  }

  public String getRole() {
    return roleLabel.getText();
  }
}
