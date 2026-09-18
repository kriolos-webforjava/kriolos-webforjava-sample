package io.github.kriolos.opos.views;

import io.github.kriolos.opos.components.Explore;

import com.webforj.component.Composite;
import com.webforj.component.layout.flexlayout.FlexAlignment;
import com.webforj.component.layout.flexlayout.FlexLayout;
import com.webforj.router.annotation.FrameTitle;
import com.webforj.router.annotation.Route;

@Route(value = "/deals", outlet = MainLayout.class)
@FrameTitle("Deals")
public class DealsView extends Composite<FlexLayout> {
  private FlexLayout self = getBoundComponent();

  public DealsView() {
    self.setHeight("100%");
    self.setAlignment(FlexAlignment.CENTER);
    self.add(new Explore("No deals in your pipeline", "briefcase", "New deal"));
  }
}
