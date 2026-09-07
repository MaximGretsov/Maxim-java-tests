package factories;

import generators.RandomModelGenerator;
import models.DepositRequest;

public class DepositRequestFactory {

    private DepositRequestFactory() {
    }

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
