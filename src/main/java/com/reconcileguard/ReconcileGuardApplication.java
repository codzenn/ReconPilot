package com.reconcileguard;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class ReconcileGuardApplication {
    public static void main(String[] args) {
        configureDatabaseEnvironment();
        SpringApplication.run(ReconcileGuardApplication.class, args);
    }

    private static void configureDatabaseEnvironment() {
        String rawUrl = firstNonBlank(System.getenv("RG_DB_URL"), System.getenv("DATABASE_URL"));
        if (isBlank(rawUrl) || rawUrl.startsWith("jdbc:")) {
            return;
        }
        if (!rawUrl.startsWith("postgresql://") && !rawUrl.startsWith("postgres://")) {
            return;
        }

        URI uri;
        try {
            uri = new URI(rawUrl);
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("Invalid database URL: " + rawUrl, ex);
        }

        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + (uri.getPort() == -1 ? 5432 : uri.getPort())
                + uri.getPath()
                + (isBlank(uri.getQuery()) ? "" : "?" + uri.getQuery());
        System.setProperty("RG_DB_URL", jdbcUrl);

        if (isBlank(System.getenv("RG_DB_DRIVER")) && isBlank(System.getProperty("RG_DB_DRIVER"))) {
            System.setProperty("RG_DB_DRIVER", "org.postgresql.Driver");
        }

        String userInfo = uri.getUserInfo();
        if (isBlank(userInfo)) {
            return;
        }

        String[] credentials = userInfo.split(":", 2);
        if (credentials.length > 0 && isBlank(System.getenv("RG_DB_USERNAME")) && isBlank(System.getProperty("RG_DB_USERNAME"))) {
            System.setProperty("RG_DB_USERNAME", decode(credentials[0]));
        }
        if (credentials.length > 1 && isBlank(System.getenv("RG_DB_PASSWORD")) && isBlank(System.getProperty("RG_DB_PASSWORD"))) {
            System.setProperty("RG_DB_PASSWORD", decode(credentials[1]));
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
