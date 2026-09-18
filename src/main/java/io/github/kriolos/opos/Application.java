package io.github.kriolos.opos;

import com.webforj.App;
import com.webforj.annotation.AppEntry;
import com.webforj.annotation.AppProfile;
import com.webforj.annotation.AppTheme;
import com.webforj.annotation.Routify;
import com.webforj.bundle.annotation.BundleEntry;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;

@AppEntry
@ApplicationScoped
@RegisterForReflection
@Routify(packages = "io.github.kriolos.opos.views")
@BundleEntry("app.css")
@AppTheme("system")
@AppProfile(name = "KriolOS POS", shortName = "KriolOS POS")
public class Application extends App {
}
