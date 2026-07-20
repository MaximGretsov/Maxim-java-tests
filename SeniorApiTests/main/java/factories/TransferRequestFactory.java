package factories;

import models.TransferRequest;

public class TransferRequestFactory {

    private TransferRequestFactory() {
    }

    public static TransferRequest transferRequest(int senderAccountId,
                                                  int receiverAccountId,
                                                  float amount) {
        return TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();
    }
}
