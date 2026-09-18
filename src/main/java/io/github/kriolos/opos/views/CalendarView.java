package io.github.kriolos.opos.views;

import io.github.kriolos.opos.components.Explore;

import com.webforj.component.Composite;
import com.webforj.component.layout.flexlayout.FlexAlignment;
import com.webforj.component.layout.flexlayout.FlexLayout;
import com.webforj.router.annotation.FrameTitle;
import com.webforj.router.annotation.Route;

@Route(value = "/calendar", outlet = MainLayout.class)
@FrameTitle("Calendar")
public class CalendarView extends Composite<FlexLayout> {
  private FlexLayout self = getBoundComponent();

  public CalendarView() {
    self.setHeight("100%");
    self.setAlignment(FlexAlignment.CENTER);
    self.add(new Explore("Nothing scheduled", "calendar-event", "New event"));
  }
}
