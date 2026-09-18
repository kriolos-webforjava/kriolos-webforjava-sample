# KriolOS POS

A minimal and ready-to-use starting point for building webforJ applications. This archetype includes the essential setup to help you launch your project quickly and focus on your app logic.

## Prerequisites

- Java 21 or newer  
- Maven 3.9+

## Getting Started

To run the application in development mode:

```bash
mvn
```

Then open [http://localhost:8080](http://localhost:8080) in your browser.

This project is preconfigured to use the **Jetty Maven Plugin**, which makes development faster.

### Live Class Updates

The project runs with hotswap turned on. The webforJ plugin attaches the class update tool to the application when it starts:

```xml
<hotswap>
  <hotswapAgent/>
</hotswap>
```

Recompile after a change, your IDE's build on save is enough, and the running application picks up the new classes. Open views update in the browser without losing their state.

Jetty's own scan mode restarts the webapp whenever compiled classes change, which would replace the in place updates with full restarts, so it stays off:

```xml
<jetty.scan>0</jetty.scan>
```

Set it to `1` to fall back to restart based reloading when hotswap is turned off.

## Running Integration Tests

To run end-to-end and integration tests:

```bash
mvn verify
```

This command:
- Starts Jetty before tests using the `jetty:start` goal
- Runs integration tests using the **Failsafe Plugin** (tests ending with `*IT.java`)
- Shuts down Jetty after tests complete


## Building for Production

To create a WAR file for deployment:

```bash
mvn clean package -Pprod
```

The WAR file will be created in `target/kriolos-opos-webforj-sidemenu-1.0-SNAPSHOT.war`

## Learn More

Explore the webforJ ecosystem through our documentation and examples:

- [Full Documentation](https://docs.webforj.com)
- [Component Overview](https://docs.webforj.com/docs/components/overview)
- [Quick Tutorial](https://docs.webforj.com/docs/introduction/tutorial/overview)
- [Advanced Topics](https://docs.webforj.com/docs/advanced/overview)
