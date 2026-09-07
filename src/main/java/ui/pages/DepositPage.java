package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import common.helpers.UiStepLogger;
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
        return UiStepLogger.log(
                "Select account " + accountId + " for deposit",
                () -> {
                    accountSelector
                            .shouldBe(visible, enabled)
                            .selectOptionByValue(
                                    String.valueOf(accountId)
                            );

                    return this;
                }
        );
    }

    public DepositPage enterAmount(float amount) {
        return UiStepLogger.log(
                "Enter deposit amount " + amount,
                () -> {
                    String amountValue =
                            Float.toString(amount);

                    amountInput
                            .shouldBe(visible, enabled)
                            .setValue(amountValue)
                            .shouldHave(exactValue(amountValue));

                    return this;
                }
        );
    }

    public DepositPage submitDeposit() {
        return UiStepLogger.log(
                "Submit deposit",
                () -> {
                    depositButton
                            .shouldBe(visible, enabled)
                            .click();

                    return this;
                }
        );
    }
}
