package ui.pages;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

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
        usernameInput.sendKeys(username);
        passwordInput.sendKeys(password);
        addUserButton.click();
        return this;
    }

    public AdminPanel shouldHaveUser(String username) {
        getAllUsers()
                .findBy(Condition.exactText(
                        USER_BADGE_TEXT_TEMPLATE.formatted(username)
                ))
                .shouldBe(Condition.visible);

        return this;
    }

    public AdminPanel shouldNotHaveUser(String username) {
        getAllUsers()
                .findBy(Condition.text(username))
                .shouldNotBe(Condition.exist);

        return this;
    }

    public ElementsCollection getAllUsers() {
        return $(Selectors.byText("All Users")).parent().findAll("li");
    }
}
