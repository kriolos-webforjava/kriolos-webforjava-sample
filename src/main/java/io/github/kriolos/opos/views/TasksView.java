package io.github.kriolos.opos.views;

import io.github.kriolos.opos.components.Explore;

import com.webforj.component.Composite;
import com.webforj.component.layout.flexlayout.FlexAlignment;
import com.webforj.component.layout.flexlayout.FlexLayout;
import com.webforj.router.annotation.FrameTitle;
import com.webforj.router.annotation.Route;

@Route(value = "/tasks", outlet = MainLayout.class)
@FrameTitle("Tasks")
public class TasksView extends Composite<FlexLayout> {
  private FlexLayout self = getBoundComponent();

  public TasksView() {
    self.setHeight("100%");
    self.setAlignment(FlexAlignment.CENTER);
    self.add(new Explore("You're all caught up", "checklist", "Create task"));
  }
}
