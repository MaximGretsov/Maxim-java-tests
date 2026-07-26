package ui.pages;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class TransferPage extends BasePage<TransferPage> {

    private final SelenideElement accountSelector =
            $(".account-selector");

    private final SelenideElement recipientNameInput =
            $(Selectors.byAttribute(
                    "placeholder",
                    "Enter recipient name"
            ));

    private final SelenideElement recipientAccountNumberInput =
            $(Selectors.byAttribute(
                    "placeholder",
                    "Enter recipient account number"
            ));

    private final SelenideElement transferAmountInput =
            $(Selectors.byAttribute(
                    "placeholder",
                    "Enter amount"
            ));

    private final SelenideElement confirmationCheckbox =
            $("input[type='checkbox']");

    private final SelenideElement sendTransferButton =
            $$("button").findBy(text("Send Transfer"));

    private final SelenideElement transferAgainButton =
            $$("button").findBy(text("Transfer Again"));

    private final SelenideElement transactionSearchInput =
            $(Selectors.byAttribute(
                    "placeholder",
                    "Enter name to find transactions"
            ));

    private final SelenideElement searchTransactionsButton =
            $$("button").findBy(text("Search Transactions"));

    private final SelenideElement transactionsList =
            $("ul.list-group");

    private final ElementsCollection transactionItems =
            $$("li.list-group-item");

    private final ElementsCollection repeatButtons =
            $$("button").filterBy(text("Repeat"));

    private final SelenideElement repeatTransferModal =
            $(".modal.show");

    private final SelenideElement repeatSenderAccountSelector =
            $(".modal.show select");

    private final SelenideElement repeatTransferAmountInput =
            $(".modal.show input[type='number']");

    private final SelenideElement repeatConfirmationCheckbox =
            $(".modal.show input[type='checkbox']");

    private final SelenideElement repeatTransferButton =
            $(".modal.show")
                    .$$("button")
                    .findBy(text("Send Transfer"));

    @Override
    public String url() {
        return "/transfer";
    }

    public TransferPage selectSenderAccount(int senderAccountId) {
        accountSelector
                .shouldBe(visible)
                .shouldBe(enabled)
                .selectOptionByValue(
                        String.valueOf(senderAccountId)
                );

        return this;
    }

    public TransferPage enterReceiverName(String receiverName) {
        recipientNameInput
                .shouldBe(visible)
                .setValue(receiverName)
                .shouldHave(exactValue(receiverName));

        return this;
    }

    public TransferPage enterReceiverAccountNumber(
            String receiverAccountNumber
    ) {
        recipientAccountNumberInput
                .shouldBe(visible)
                .setValue(receiverAccountNumber)
                .shouldHave(exactValue(receiverAccountNumber));

        return this;
    }

    public TransferPage enterTransferAmount(float transferAmount) {
        String transferAmountValue =
                Float.toString(transferAmount);

        transferAmountInput
                .shouldBe(visible)
                .setValue(transferAmountValue)
                .shouldHave(exactValue(transferAmountValue));

        return this;
    }

    public TransferPage confirmTransferDetails() {
        confirmationCheckbox
                .shouldBe(visible)
                .setSelected(true)
                .shouldBe(selected);

        return this;
    }

    public TransferPage submitTransfer() {
        sendTransferButton
                .shouldBe(visible)
                .shouldBe(enabled)
                .click();

        return this;
    }

    public TransferPage openTransferAgain() {
        transferAgainButton
                .shouldBe(visible)
                .shouldBe(enabled)
                .click();

        return this;
    }

    public TransferPage searchTransactions(String username) {
        transactionSearchInput
                .shouldBe(visible)
                .setValue(username)
                .shouldHave(exactValue(username));

        searchTransactionsButton
                .shouldBe(visible)
                .shouldBe(enabled)
                .click();

        return this;
    }

    public TransferPage openRepeatTransferFor(
            String transactionType
    ) {
        transactionItems
                .findBy(text(transactionType))
                .shouldBe(visible)
                .$("button")
                .shouldBe(visible)
                .shouldBe(enabled)
                .click();

        repeatTransferModal
                .shouldBe(visible)
                .shouldHave(text("Repeat Transfer"));

        return this;
    }

    public TransferPage selectRepeatSenderAccount(
            int senderAccountId
    ) {
        repeatSenderAccountSelector
                .shouldBe(visible)
                .shouldBe(enabled)
                .selectOptionByValue(
                        String.valueOf(senderAccountId)
                );

        return this;
    }

    public TransferPage enterRepeatTransferAmount(float amount) {
        String amountValue = Float.toString(amount);

        repeatTransferAmountInput
                .shouldBe(visible)
                .setValue(amountValue)
                .shouldHave(exactValue(amountValue));

        return this;
    }

    public TransferPage confirmRepeatTransferDetails() {
        repeatConfirmationCheckbox
                .shouldBe(visible)
                .setSelected(true)
                .shouldBe(selected);

        return this;
    }

    public TransferPage submitRepeatTransfer() {
        repeatTransferButton
                .shouldBe(visible)
                .shouldBe(enabled)
                .click();

        return this;
    }

    public TransferPage checkSearchResultsAreEmpty() {
        transactionsList.should(exist);

        transactionItems.shouldHave(size(0));
        repeatButtons.shouldHave(size(0));

        return this;
    }
}