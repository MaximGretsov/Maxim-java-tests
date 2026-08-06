package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import common.helpers.UiStepLogger;

import static com.codeborne.selenide.Selenide.$;

public class LoginPage extends BasePage<LoginPage>{
    private SelenideElement button =  $("button");

    @Override
    public String url() {
        return "/login";
    }

    public LoginPage login(String username, String password){
        return UiStepLogger.log(
                "Login as user " + username,
                () -> {
                    usernameInput.sendKeys(username);
                    passwordInput.sendKeys(password);
                    button.click();
                    return this;
                }
        );
    }
}
