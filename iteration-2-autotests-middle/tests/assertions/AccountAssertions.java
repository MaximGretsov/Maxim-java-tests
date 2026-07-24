package iteration2.assertions;

import io.restassured.specification.RequestSpecification;
import models.AccountResponse;
import org.assertj.core.api.SoftAssertions;
import request.steps.AccountSteps;

import static TestData.AccountTestData.FLOAT_ASSERTION_OFFSET;
import static org.assertj.core.data.Offset.offset;

public final class AccountAssertions {

    private AccountAssertions() {
    }

    public static void assertAccountBalance(
            SoftAssertions softy,
            RequestSpecification userSpec,
            int accountId,
            float expectedBalance
    ) {
        AccountResponse account =
                AccountSteps.getAccountById(userSpec, accountId);

        softy.assertThat(account.getBalance())
                .as("Balance of account with id %s", accountId)
                .isCloseTo(
                        expectedBalance,
                        offset(FLOAT_ASSERTION_OFFSET)
                );
    }
}
