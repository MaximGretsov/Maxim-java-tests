package api.assertions;

import api.models.TransferRequest;
import api.models.TransferResponse;
import api.models.comparison.ModelAssertions;
import org.assertj.core.api.SoftAssertions;

import static api.specs.ResponseSpecs.TRANSFER_SUCCESS_MESSAGE;

public class TransferAssertions {

    private TransferAssertions() {
    }

    public static void assertSuccessfulTransferResponse(
            SoftAssertions softy,
            TransferRequest transferRequest,
            TransferResponse transferResponse
    ) {
        ModelAssertions.assertThatModels(
                transferRequest,
                transferResponse
        ).match();

        softy.assertThat(transferResponse.getMessage())
                .isEqualTo(TRANSFER_SUCCESS_MESSAGE);
    }
}
