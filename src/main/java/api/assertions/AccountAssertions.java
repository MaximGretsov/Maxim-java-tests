package api.assertions;

import api.requests.steps.UserSteps;
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

    public static void assertAccountIsEmpty(
            SoftAssertions softy,
            RequestSpecification userSpec,
            int accountId
    ) {
        assertAccountIsEmpty(
                softy,
                AccountSteps.getAccountById(userSpec, accountId),
                accountId
        );
    }

    public static void assertAccountIsEmpty(
            SoftAssertions softy,
            UserSteps userSteps,
            int accountId
    ) {
        assertAccountIsEmpty(
                softy,
                userSteps.getAccountById(accountId),
                accountId
        );
    }

    public static void assertAccountIsEmpty(
            SoftAssertions softy,
            AccountResponse account,
            int accountId
    ) {
        ModelAssertions
                .assertThatModels(
                        expectedEmptyAccount(accountId),
                        account
                )
                .match();

        softy.assertThat(account.getTransactions())
                .isEmpty();
    }

    public static void assertAccountAfterSuccessfulDeposit(
            SoftAssertions softy,
            RequestSpecification userSpec,
            DepositRequest depositRequest
    ) {
        assertAccountAfterSuccessfulDeposit(
                softy,
                AccountSteps.getAccountById(
                        userSpec,
                        depositRequest.getId()
                ),
                depositRequest
        );
    }

    public static void assertAccountAfterSuccessfulDeposit(
            SoftAssertions softy,
            UserSteps userSteps,
            DepositRequest depositRequest
    ) {
        assertAccountAfterSuccessfulDeposit(
                softy,
                userSteps.getAccountById(depositRequest.getId()),
                depositRequest
        );
    }

    public static void assertAccountAfterSuccessfulDeposit(
            SoftAssertions softy,
            AccountResponse account,
            DepositRequest depositRequest
    ) {
        ModelAssertions
                .assertThatModels(
                        depositRequest,
                        account
                )
                .match();

        softy.assertThat(account.getTransactions())
                .isNotEmpty();
    }

    public static void assertAccountBalance(
            SoftAssertions softy,
            RequestSpecification userSpec,
            int accountId,
            float expectedBalance
    ) {
        AccountResponse account =
                AccountSteps.getAccountById(
                        userSpec,
                        accountId
                );

        assertAccountBalance(
                softy,
                account,
                accountId,
                expectedBalance
        );
    }

    public static void assertAccountBalance(
            SoftAssertions softy,
            UserSteps userSteps,
            int accountId,
            float expectedBalance
    ) {
        AccountResponse account =
                userSteps.getAccountById(accountId);

        assertAccountBalance(
                softy,
                account,
                accountId,
                expectedBalance
        );
    }

    public static void assertAccountBalance(
            SoftAssertions softy,
            AccountResponse account,
            int accountId,
            float expectedBalance
    ) {
        softy.assertThat(account.getId())
                .isEqualTo(accountId);

        softy.assertThat(account.getBalance())
                .isCloseTo(
                        expectedBalance,
                        within(FLOAT_ASSERTION_OFFSET)
                );
    }

    private static AccountResponse expectedEmptyAccount(
            int accountId
    ) {
        return new AccountResponse(
                accountId,
                null,
                EMPTY_ACCOUNT_BALANCE,
                List.of()
        );
    }
}