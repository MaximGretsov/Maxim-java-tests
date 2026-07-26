package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;

@Getter
public class UserDashboard extends BasePage<UserDashboard>{
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
}
