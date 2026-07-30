package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import common.utils.RetryUtils;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;

public class EditProfilePage extends BasePage<EditProfilePage>{
    private SelenideElement newNameInput = $(Selectors.byAttribute("placeholder","Enter new name"));
    private SelenideElement saveChangesButton = $$("button").findBy(Condition.text("Save Changes"));
    private SelenideElement homeButton = $$("button").findBy(Condition.text("Home"));
    @Override
    public String url() {
        return "/edit-profile";
    }

    public EditProfilePage changeName(String newName) {
        newNameInput.shouldBe(visible, enabled);

        AtomicInteger stableChecks = new AtomicInteger();

        new WebDriverWait(
                WebDriverRunner.getWebDriver(),
                Duration.ofSeconds(5)
        )
                .pollingEvery(Duration.ofMillis(200))
                .until(driver -> {
                    String actualValue = newNameInput.getValue();

                    if (!newName.equals(actualValue)) {
                        newNameInput.setValue(newName);
                        stableChecks.set(0);
                        return false;
                    }

                    return stableChecks.incrementAndGet() >= 2;
                });

        saveChangesButton
                .shouldBe(visible, enabled)
                .click();

        return this;
    }

    public UserDashboard openUserDashboard(){
        homeButton
                .shouldBe(visible)
                .shouldBe(enabled)
                .click();

        return page(UserDashboard.class);
    }
}
