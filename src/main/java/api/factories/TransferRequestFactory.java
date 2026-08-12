package api.factories;

import api.models.TransferRequest;

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

    public static TransferRequest fraudCheckTransferRequest(
            int senderAccountId,
            int receiverAccountId,
            double amount
    ) {
        return TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .description("Test transfer with fraud check")
                .build();
    }
}
