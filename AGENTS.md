# AGENTS.md

This is a webforJ project: the UI is built by composing webforJ components in Java.

## Commands

- `mvn` — dev mode. The default goal (`compile webforj:watch <run>`) compiles the frontend, watches `src/main/frontend`, and serves the app.
- `mvn verify` — run the `*IT` integration tests (Playwright).
- `mvn -Pprod package` — production build into `target/`.

Java version, dependencies, plugins, and the run goal are declared in `pom.xml` — read them there instead of assuming.

## Layout

- `src/main/java/.../Application.java` — entry point, `extends App`. `@Routify` scans the `views` package and `@BundleEntry("app.css")` loads the app stylesheet.
- `src/main/java/.../views/` — routes. Each is a `@Route`-annotated `Composite`.
- `src/main/frontend/` — frontend sources compiled by the webforJ Maven plugin. `app.css` lives here.
- `src/main/resources/` — `webforj.conf`, or `application.properties` on Spring, for runtime config.

## webforJ MCP server

Already configured in this repo at `https://mcp.webforj.com/mcp`.

- Resolve the webforJ version from `pom.xml`, and scope every answer to it.
- Look up webforJ classes, methods, and annotations through the server — don't guess them.
- Validate every `--dwc-*` CSS token through the server before using it.

## Testing

Integration tests use Playwright and live in `src/test/java/.../views/` as `<View>IT.java`. `mvn verify` runs them. A test launches Chromium, navigates to `http://localhost:<port>/`, and asserts with `PlaywrightAssertions.assertThat(...)`. Name the class `*IT` so the failsafe plugin runs it, and add one per view.

## Do's and Don'ts

- **Do** style with DWC tokens (`--dwc-*`) and set the app color via `--dwc-color-primary-seed` in `src/main/frontend/app.css`.
- **Do** follow the DWC design system — https://dwc.style/docs/design.md is the full token catalog (colors, typography, spacing, shadows, motion) and component recipes. Every value is a `var(--dwc-*)` token you consume directly.
- **Do** add a `<View>IT` test for each new view, and run `mvn verify` before finishing.
- **Don't** hardcode colors, sizes, or raw CSS values — use `--dwc-*` tokens.
- **Don't** guess webforJ APIs or token names — resolve them through the MCP server.
- **Don't** edit anything under `target/` or other generated output.
- **Don't** add dependencies or change the build without checking `pom.xml` first.
