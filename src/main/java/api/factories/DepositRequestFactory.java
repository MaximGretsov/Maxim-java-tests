package api.factories;

import api.generators.RandomModelGenerator;
import api.models.DepositRequest;

public class DepositRequestFactory {
    // factory
    private DepositRequestFactory() {
    }

    // valid Deposit Request
    public static DepositRequest validDepositRequest(int accountId) {
        return DepositRequest.builder()
                .id(accountId)
                .balance(RandomModelGenerator.generateValidDepositAmount())
                .build();
    }

    public static DepositRequest depositRequestWithAmount(int accountId, float depositAmount) {
        return DepositRequest.builder()
                .id(accountId)
                .balance(depositAmount)
                .build();
    }
}
