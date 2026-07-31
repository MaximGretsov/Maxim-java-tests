package api.configs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

public final class Config {

    private static final Config INSTANCE = new Config();

    private final Properties properties = new Properties();

    private Config() {
        try (InputStream input = Config.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {

            if (input == null) {
                throw new IllegalStateException(
                        "File config.properties not found in resources"
                );
            }

            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load config.properties",
                    e
            );
        }
    }

    public static String getProperty(String key) {
        // Приоритет 1: параметр JVM, например -DuiBaseUrl=http://localhost:3000
        String systemValue = System.getProperty(key);

        if (isNotBlank(systemValue)) {
            return systemValue;
        }

        // Приоритет 2: переменная окружения.
        // uiBaseUrl -> UIBASEURL
        // apiBaseUrl -> APIBASEURL
        // apiVersion -> APIVERSION
        String environmentKey = key
                .toUpperCase(Locale.ROOT)
                .replace('.', '_')
                .replace('-', '_');

        String environmentValue = System.getenv(environmentKey);

        if (isNotBlank(environmentValue)) {
            return environmentValue;
        }

        // Приоритет 3: локальный config.properties
        String propertyValue = INSTANCE.properties.getProperty(key);

        if (isNotBlank(propertyValue)) {
            return propertyValue.trim();
        }

        throw new IllegalArgumentException(
                "Configuration property not found: " + key
        );
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
