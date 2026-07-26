package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

@Getter
public class DepositPage extends BasePage<DepositPage>{
    private SelenideElement accountSelector = $(".account-selector");
    private SelenideElement amountInput = $(Selectors.byAttribute("placeholder", "Enter amount"));
    private SelenideElement depositButton = $$("button").findBy(text("Deposit"));

    @Override
    public String url() {
        return "/deposit";
    }

    public DepositPage selectAccount(int accountId) {
        accountSelector
                .shouldBe(visible)
                .shouldBe(enabled)
                .selectOptionByValue(String.valueOf(accountId));

        return this;
    }

    public DepositPage enterAmount(float amount){
        String amountValue = Float.toString(amount);

        amountInput
                .setValue(amountValue)
                .shouldHave(exactValue(amountValue));

        return this;
    }

    public DepositPage submitDeposit() {
        depositButton
                .shouldBe(visible)
                .shouldBe(enabled)
                .click();

        return this;
    }
}
