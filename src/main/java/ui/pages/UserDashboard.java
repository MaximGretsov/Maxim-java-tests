package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;
import org.openqa.selenium.Alert;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

@Getter
public class UserDashboard extends BasePage<UserDashboard>{
    private static final String WELCOME_TEXT_TEMPLATE = "Welcome, %s!";
    private static final String DEFAULT_PROFILE_NAME = "noname";
    private SelenideElement welcomeText = $(Selectors.byClassName("welcome-text"));
    private SelenideElement createNewAccount = $(Selectors.byText("➕ Create New Account"));
    private SelenideElement depositMoneyButton = $$("button").findBy(text("Deposit Money"));
    private SelenideElement transferButton = $$("button").findBy(text("Make a Transfer"));
    private SelenideElement userInfo =  $(".user-info");
    private SelenideElement welcomeTextSpan = $(".welcome-text span");

    @Override
    public String url() {
        return "/dashboard";
    }

    public UserDashboard createNewAccount(){
        createNewAccount.click();
        return this;
    }

    public DepositPage openDepositPage(){
        depositMoneyButton
                .shouldBe(visible)
                .shouldBe(enabled)
                .click();

        return page(DepositPage.class);
    }

    public TransferPage openTransferPage(){
        transferButton
                .shouldBe(visible)
                .shouldBe(enabled)
                .click();

        return page(TransferPage.class);
    }

    public EditProfilePage openEditProfile(){
        userInfo
                .shouldBe(visible)
                .shouldBe(enabled)
                .click();

        return page(EditProfilePage.class);
    }

    public UserDashboard checkDisplayedName(String expectedName){
         welcomeTextSpan
                .shouldBe(visible)
                .shouldHave(exactText(expectedName));
         return this;
    }

    public String checkNewAccountCreatedAlertAndAccept() {
        Alert alert = switchTo().alert();
        String actualMessage = alert.getText();
        String expectedMessage = BankAlert.NEW_ACCOUNT_CREATED.getMessage();

        assertThat(actualMessage)
                .startsWith(expectedMessage);

        String accountNumber = actualMessage
                .substring(expectedMessage.length())
                .trim();

        alert.accept();

        return accountNumber;
    }

    public UserDashboard shouldHaveWelcomeText(String profileName) {
        welcomeText
                .shouldBe(Condition.visible)
                .shouldHave(Condition.exactText(
                        WELCOME_TEXT_TEMPLATE.formatted(profileName)
                ));

        return this;
    }

    public UserDashboard shouldHaveDefaultWelcomeText() {
        return shouldHaveWelcomeText(DEFAULT_PROFILE_NAME);
    }
}
