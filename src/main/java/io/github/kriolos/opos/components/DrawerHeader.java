package io.github.kriolos.opos.components;

import com.webforj.component.Composite;
import com.webforj.component.html.elements.H1;
import com.webforj.component.html.elements.Paragraph;
import com.webforj.component.layout.flexlayout.FlexDirection;
import com.webforj.component.layout.flexlayout.FlexLayout;

public class DrawerHeader extends Composite<FlexLayout> {
  private FlexLayout self = getBoundComponent();

  public DrawerHeader() {
    self.setDirection(FlexDirection.COLUMN);
    self.setSpacing("0");

    H1 title = new H1("Acme CRM");
    title.setStyle("margin", "0");

    Paragraph email = new Paragraph("john@acme.com");
    email.setStyle("color", "var(--dwc-color-gray-text-light)");
    email.setStyle("font-size", "var(--dwc-font-size-xs)");
    email.setStyle("margin", "0");

    self.add(title, email);
  }
}
