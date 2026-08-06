package ui.pages;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import common.helpers.StepLogger;
import common.helpers.UiStepLogger;
import lombok.Getter;
import common.utils.RetryUtils;
import ui.elements.UserBage;

import java.util.List;

import static com.codeborne.selenide.Selenide.$;

@Getter
public class AdminPanel extends BasePage<AdminPanel>{
    private SelenideElement adminPanelText =  $(Selectors.byText("Admin Panel"));
    private SelenideElement addUserButton = $(Selectors.byText("Add User"));
    private static final String USER_BADGE_TEXT_TEMPLATE = "%s\nUSER";

    @Override
    public String url() {
        return "/admin";
    }

    public AdminPanel createUser(String username, String password) {
        return UiStepLogger.log(
                "Admin creates user " + username,
                () -> {
                    usernameInput.sendKeys(username);
                    passwordInput.sendKeys(password);
                    addUserButton.click();
                    return this;
                }
        );
    }

    public List<UserBage> getAllUsers() {
        return UiStepLogger.log("Get all users from Dashboard",  () -> {
            ElementsCollection elementsCollection = $(Selectors.byText("All Users")).parent().findAll("li");
            return generatePageElements(elementsCollection, UserBage::new);
        });
    }

    public UserBage findUserByUsername(String username) {
        return UiStepLogger.log(
                "Find user by username: " + username,
                () -> RetryUtils.retry(
                "Find user by username: " + username,
                () -> getAllUsers()
                        .stream()
                        .filter(user -> user.getUsername().equals(username))
                        .findAny()
                        .orElse(null),
                result -> result != null,
                3,
                1000)
        );
    }
}
