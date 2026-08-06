package ui.pages;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import common.helpers.UiStepLogger;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class TransferPage extends BasePage<TransferPage> {
    private static final String INCOMING_TRANSFER_TYPE = "TRANSFER_IN";

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

    public TransferPage selectSenderAccount(
            int senderAccountId
    ) {
        return UiStepLogger.log(
                "Select sender account " + senderAccountId,
                () -> {
                    accountSelector
                            .shouldBe(visible, enabled)
                            .selectOptionByValue(
                                    String.valueOf(
                                            senderAccountId
                                    )
                            );

                    return this;
                }
        );
    }

    public TransferPage enterReceiverName(
            String receiverName
    ) {
        return UiStepLogger.log(
                "Enter receiver name " + receiverName,
                () -> {
                    recipientNameInput
                            .shouldBe(visible, enabled)
                            .setValue(receiverName)
                            .shouldHave(
                                    exactValue(receiverName)
                            );

                    return this;
                }
        );
    }

    public TransferPage enterReceiverAccountNumber(
            String receiverAccountNumber
    ) {
        return UiStepLogger.log(
                "Enter receiver account number "
                        + receiverAccountNumber,
                () -> {
                    recipientAccountNumberInput
                            .shouldBe(visible, enabled)
                            .setValue(
                                    receiverAccountNumber
                            )
                            .shouldHave(
                                    exactValue(
                                            receiverAccountNumber
                                    )
                            );

                    return this;
                }
        );
    }

    public TransferPage enterTransferAmount(
            float transferAmount
    ) {
        return UiStepLogger.log(
                "Enter transfer amount " + transferAmount,
                () -> {
                    String transferAmountValue =
                            Float.toString(transferAmount);

                    transferAmountInput
                            .shouldBe(visible, enabled)
                            .setValue(transferAmountValue)
                            .shouldHave(
                                    exactValue(
                                            transferAmountValue
                                    )
                            );

                    return this;
                }
        );
    }

    public TransferPage confirmTransferDetails() {
        return UiStepLogger.log(
                "Confirm transfer details",
                () -> {
                    confirmationCheckbox
                            .shouldBe(visible, enabled)
                            .setSelected(true)
                            .shouldBe(selected);

                    return this;
                }
        );
    }

    public TransferPage submitTransfer() {
        return UiStepLogger.log(
                "Submit transfer",
                () -> {
                    sendTransferButton
                            .shouldBe(visible, enabled)
                            .click();

                    return this;
                }
        );
    }

    public TransferPage openTransferAgain() {
        return UiStepLogger.log(
                "Open transfer again form",
                () -> {
                    transferAgainButton
                            .shouldBe(visible, enabled)
                            .click();

                    return this;
                }
        );
    }

    public TransferPage searchTransactions(
            String username
    ) {
        return UiStepLogger.log(
                "Search transactions for user " + username,
                () -> {
                    transactionSearchInput
                            .shouldBe(visible, enabled)
                            .setValue(username)
                            .shouldHave(exactValue(username));

                    searchTransactionsButton
                            .shouldBe(visible, enabled)
                            .click();

                    return this;
                }
        );
    }

    public TransferPage openRepeatTransferFor(
            String transactionType
    ) {
        return UiStepLogger.log(
                "Open repeat transfer for "
                        + transactionType,
                () -> {
                    transactionItems
                            .findBy(text(transactionType))
                            .shouldBe(visible)
                            .$("button")
                            .shouldBe(visible, enabled)
                            .click();

                    repeatTransferModal
                            .shouldBe(visible)
                            .shouldHave(
                                    text("Repeat Transfer")
                            );

                    return this;
                }
        );
    }

    public TransferPage selectRepeatSenderAccount(
            int senderAccountId
    ) {
        return UiStepLogger.log(
                "Select repeat sender account "
                        + senderAccountId,
                () -> {
                    repeatSenderAccountSelector
                            .shouldBe(visible, enabled)
                            .selectOptionByValue(
                                    String.valueOf(
                                            senderAccountId
                                    )
                            );

                    return this;
                }
        );
    }

    public TransferPage enterRepeatTransferAmount(
            float amount
    ) {
        return UiStepLogger.log(
                "Enter repeat transfer amount " + amount,
                () -> {
                    String amountValue =
                            Float.toString(amount);

                    repeatTransferAmountInput
                            .shouldBe(visible, enabled)
                            .setValue(amountValue)
                            .shouldHave(
                                    exactValue(amountValue)
                            );

                    return this;
                }
        );
    }

    public TransferPage confirmRepeatTransferDetails() {
        return UiStepLogger.log(
                "Confirm repeat transfer details",
                () -> {
                    repeatConfirmationCheckbox
                            .shouldBe(visible, enabled)
                            .setSelected(true)
                            .shouldBe(selected);

                    return this;
                }
        );
    }

    public TransferPage submitRepeatTransfer() {
        return UiStepLogger.log(
                "Submit repeat transfer",
                () -> {
                    repeatTransferButton
                            .shouldBe(visible, enabled)
                            .click();

                    return this;
                }
        );
    }

    public TransferPage checkSearchResultsAreEmpty() {
        return UiStepLogger.log(
                "Check that transaction search is empty",
                () -> {
                    transactionsList.should(exist);
                    transactionItems.shouldHave(size(0));
                    repeatButtons.shouldHave(size(0));

                    return this;
                }
        );
    }

    public TransferPage openIncomingTransferForRepeat() {
        return openRepeatTransferFor(
                INCOMING_TRANSFER_TYPE
        );
    }
}