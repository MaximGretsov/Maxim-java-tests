package api.assertions;

import io.restassured.specification.RequestSpecification;
import api.models.AccountResponse;
import api.models.DepositRequest;
import api.models.comparison.ModelAssertions;
import org.assertj.core.api.SoftAssertions;
import api.requests.steps.AccountSteps;

import java.util.List;

import static org.assertj.core.api.Assertions.within;
import static api.testdata.AccountTestData.EMPTY_ACCOUNT_BALANCE;
import static api.testdata.AccountTestData.FLOAT_ASSERTION_OFFSET;

public class AccountAssertions {

    private AccountAssertions() {
    }

    public static void assertAccountIsEmpty(SoftAssertions softy,
                                            RequestSpecification userSpec,
                                            int accountId) {
        AccountResponse account = AccountSteps.getAccountById(userSpec, accountId);

        ModelAssertions.assertThatModels(expectedEmptyAccount(accountId), account).match();

        softy.assertThat(account.getTransactions())
                .isEmpty();
    }

    public static void assertAccountAfterSuccessfulDeposit(SoftAssertions softy,
                                                           RequestSpecification userSpec,
                                                           DepositRequest depositRequest) {
        AccountResponse account = AccountSteps.getAccountById(userSpec, depositRequest.getId());

        ModelAssertions.assertThatModels(depositRequest, account).match();

        softy.assertThat(account.getTransactions())
                .isNotEmpty();
    }

    public static void assertAccountBalance(SoftAssertions softy,
                                            RequestSpecification userSpec,
                                            int accountId,
                                            float expectedBalance) {
        AccountResponse account = AccountSteps.getAccountById(userSpec, accountId);

        softy.assertThat(account.getId())
                .isEqualTo(accountId);

        softy.assertThat(account.getBalance())
                .isCloseTo(expectedBalance, within(FLOAT_ASSERTION_OFFSET));
    }

    private static AccountResponse expectedEmptyAccount(int accountId) {
        return new AccountResponse(accountId, null, EMPTY_ACCOUNT_BALANCE, List.of());
    }
}
