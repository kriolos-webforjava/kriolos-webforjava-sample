package io.github.kriolos.opos.views;

import io.github.kriolos.opos.components.Explore;

import com.webforj.component.Composite;
import com.webforj.component.layout.flexlayout.FlexAlignment;
import com.webforj.component.layout.flexlayout.FlexLayout;
import com.webforj.router.annotation.FrameTitle;
import com.webforj.router.annotation.Route;

@Route(value = "/reports", outlet = MainLayout.class)
@FrameTitle("Reports")
public class ReportsView extends Composite<FlexLayout> {
  private FlexLayout self = getBoundComponent();

  public ReportsView() {
    self.setHeight("100%");
    self.setAlignment(FlexAlignment.CENTER);
    self.add(new Explore("No reports yet", "chart-bar", "Build report"));
  }
}
