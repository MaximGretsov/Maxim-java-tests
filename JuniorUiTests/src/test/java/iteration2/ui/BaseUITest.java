package iteration2.ui;

import com.codeborne.selenide.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import java.util.Map;

import static com.codeborne.selenide.Selenide.closeWebDriver;

public class BaseUITest {
    @BeforeAll
public static void setUpSelenoid(){
    Configuration.remote = "http://localhost:4444/wd/hub";
    Configuration.baseUrl = "http://192.168.3.11:3000";
    Configuration.browser = "chrome";
    Configuration.browserSize = "1920x1080";

    Configuration.browserCapabilities.setCapability("selenoid:options",
            Map.of("enableVNC",true, "enableLog", true));
}

    @AfterEach
    public void tearDown() {
        closeWebDriver();
    }
}
