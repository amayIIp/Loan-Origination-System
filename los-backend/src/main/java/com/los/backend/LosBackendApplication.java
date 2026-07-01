package com.los.backend;

// Spring Boot's entry point annotation — it does three things at once:
// 1. Marks this class as a Spring configuration source
// 2. Enables automatic component scanning (Spring finds all @Controller, @Service, etc.)
// 3. Enables Spring Boot's auto-configuration (sets up MongoDB, Tomcat, Jackson, etc.)
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication — the single annotation that bootstraps the entire application.
// Without it, Spring would not know to scan for components or auto-configure anything.
@SpringBootApplication
public class LosBackendApplication {

    /**
     * Application entry point — JVM calls main() when we run the JAR.
     * SpringApplication.run() starts the embedded Tomcat server, connects to MongoDB,
     * registers all our REST controllers, and begins listening for HTTP requests.
     *
     * @param args command-line arguments (e.g., --server.port=9090 to override config)
     */
    public static void main(String[] args) {
        // Hand control over to Spring Boot — it reads application.yml,
        // wires everything together, and starts the web server on port 8080.
        SpringApplication.run(LosBackendApplication.class, args);
    }
}
