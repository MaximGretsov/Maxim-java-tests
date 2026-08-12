package iteration2.api.testsWithMock;

import api.generators.RandomModelGenerator;
import api.models.*;
import api.models.comparison.ModelAssertions;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.ValidatedCrudRequester;
import api.requests.steps.AccountSteps;
import api.specs.ResponseSpecs;
import common.annotations.FraudCheckMock;
import common.enums.FraudMockBehavior;
import io.restassured.specification.RequestSpecification;
import iteration2.api.BaseTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static api.factories.TransferRequestFactory.fraudCheckTransferRequest;

@ExtendWith({FraudCheckWireMockExtension.class})
public class TransferWithFraudCheckTest extends BaseTest {

    @Test
    @FraudCheckMock(
            status = "SUCCESS",
            decision = "APPROVED",
            riskScore = 0.2,
            reason = "Low risk transaction",
            requiresManualReview = false,
            additionalVerificationRequired = false
    )
    public void testTransferWithFraudCheck() {
        RequestSpecification senderUserSpec = createUserSpecForTest();

        int senderAccountId = AccountSteps.createAccount(senderUserSpec);

        AccountSteps.prepareAccountWithSmallBalance(senderUserSpec, senderAccountId);

        RequestSpecification receiverUserSpec = createUserSpecForTest();

        int receiverAccountId = AccountSteps.createAccount(receiverUserSpec);

        double transferAmount = RandomModelGenerator.generateValidDepositAmount();

        TransferRequest transferRequest = fraudCheckTransferRequest(
                senderAccountId,
                receiverAccountId,
                transferAmount
        );

        TransferResponse actualResponse =
                new ValidatedCrudRequester<TransferResponse>(
                        senderUserSpec,
                        Endpoint.TRANSFER_WITH_FRAUD_CHECK,
                        ResponseSpecs.requestReturnsOk()
                ).post(transferRequest);

        TransferResponse expectedResponse = TransferResponse.builder()
                .status("APPROVED")
                .message("Transfer approved and processed immediately")
                .amount(transferAmount)
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .fraudRiskScore(0.2)
                .fraudReason("Low risk transaction")
                .requiresManualReview(false)
                .requiresVerification(false)
                .build();

        ModelAssertions.assertThatModels(expectedResponse, actualResponse).match();
    }


    @Test
    @FraudCheckMock(
            status = "SUCCESS",
            decision = "BLOCKED",
            riskScore = 0.9,
            reason = "High risk transaction",
            requiresManualReview = false,
            additionalVerificationRequired = false
    )
    public void transferIsBlockedWhenFraudCheckBlocks() {
        RequestSpecification senderUserSpec = createUserSpecForTest();

        int senderAccountId = AccountSteps.createAccount(senderUserSpec);

        AccountSteps.prepareAccountWithSmallBalance(senderUserSpec, senderAccountId);

        RequestSpecification receiverUserSpec = createUserSpecForTest();

        int receiverAccountId = AccountSteps.createAccount(receiverUserSpec);

        double transferAmount = RandomModelGenerator.generateValidDepositAmount();

        TransferRequest transferRequest = fraudCheckTransferRequest(
                senderAccountId,
                receiverAccountId,
                transferAmount
        );

        TransferResponse actualResponse =
                new ValidatedCrudRequester<TransferResponse>(
                        senderUserSpec,
                        Endpoint.TRANSFER_WITH_FRAUD_CHECK,
                        ResponseSpecs.requestReturnsOk()
                ).post(transferRequest);

        TransferResponse expectedResponse = TransferResponse.builder()
                .status("BLOCKED")
                .message("Transfer blocked due to fraud detection")
                .amount(transferAmount)
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .fraudRiskScore(0.9)
                .fraudReason("High risk transaction")
                .requiresManualReview(false)
                .requiresVerification(false)
                .build();

        ModelAssertions.assertThatModels(expectedResponse, actualResponse).match();
    }


    @Test
    @FraudCheckMock(
            status = "SUCCESS",
            decision = "REVIEW_REQUIRED",
            riskScore = 0.6,
            reason = "Suspicious transaction",
            requiresManualReview = false,
            additionalVerificationRequired = false
    )
    public void transferRequiresManualReviewWhenFraudDecisionRequiresReview() {
        RequestSpecification senderUserSpec = createUserSpecForTest();

        int senderAccountId = AccountSteps.createAccount(senderUserSpec);

        AccountSteps.prepareAccountWithSmallBalance(senderUserSpec, senderAccountId);

        RequestSpecification receiverUserSpec = createUserSpecForTest();

        int receiverAccountId = AccountSteps.createAccount(receiverUserSpec);

        double transferAmount = RandomModelGenerator.generateValidDepositAmount();

        TransferRequest transferRequest = fraudCheckTransferRequest(
                senderAccountId,
                receiverAccountId,
                transferAmount
        );

        TransferResponse actualResponse =
                new ValidatedCrudRequester<TransferResponse>(
                        senderUserSpec,
                        Endpoint.TRANSFER_WITH_FRAUD_CHECK,
                        ResponseSpecs.requestReturnsOk()
                ).post(transferRequest);

        TransferResponse expectedResponse = TransferResponse.builder()
                .status("MANUAL_REVIEW_REQUIRED")
                .message("Transfer requires manual review")
                .amount(transferAmount)
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .fraudRiskScore(0.6)
                .fraudReason("Suspicious transaction")
                .requiresManualReview(false)
                .requiresVerification(false)
                .build();

        ModelAssertions.assertThatModels(expectedResponse, actualResponse).match();
    }


    @Test
    @FraudCheckMock(
            status = "SUCCESS",
            decision = "APPROVED",
            riskScore = 0.6,
            reason = "Manual review required",
            requiresManualReview = true,
            additionalVerificationRequired = false
    )
    public void transferRequiresManualReviewWhenFraudRequiresManualReview() {
        RequestSpecification senderUserSpec = createUserSpecForTest();

        int senderAccountId = AccountSteps.createAccount(senderUserSpec);

        AccountSteps.prepareAccountWithSmallBalance(senderUserSpec, senderAccountId);

        RequestSpecification receiverUserSpec = createUserSpecForTest();

        int receiverAccountId = AccountSteps.createAccount(receiverUserSpec);

        double transferAmount = RandomModelGenerator.generateValidDepositAmount();

        TransferRequest transferRequest = fraudCheckTransferRequest(
                senderAccountId,
                receiverAccountId,
                transferAmount
        );

        TransferResponse actualResponse =
                new ValidatedCrudRequester<TransferResponse>(
                        senderUserSpec,
                        Endpoint.TRANSFER_WITH_FRAUD_CHECK,
                        ResponseSpecs.requestReturnsOk()
                ).post(transferRequest);

        TransferResponse expectedResponse = TransferResponse.builder()
                .status("MANUAL_REVIEW_REQUIRED")
                .message("Transfer requires manual review")
                .amount(transferAmount)
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .fraudRiskScore(0.6)
                .fraudReason("Manual review required")
                .requiresManualReview(true)
                .requiresVerification(false)
                .build();

        ModelAssertions.assertThatModels(expectedResponse, actualResponse).match();
    }


    @Test
    @FraudCheckMock(
            status = "SUCCESS",
            decision = "VERIFICATION_REQUIRED",
            riskScore = 0.7,
            reason = "Additional verification required",
            requiresManualReview = false,
            additionalVerificationRequired = false
    )
    public void transferRequiresVerificationWhenFraudDecisionRequiresVerification() {
        RequestSpecification senderUserSpec = createUserSpecForTest();

        int senderAccountId = AccountSteps.createAccount(senderUserSpec);

        AccountSteps.prepareAccountWithSmallBalance(senderUserSpec, senderAccountId);

        RequestSpecification receiverUserSpec = createUserSpecForTest();

        int receiverAccountId = AccountSteps.createAccount(receiverUserSpec);

        double transferAmount = RandomModelGenerator.generateValidDepositAmount();

        TransferRequest transferRequest = fraudCheckTransferRequest(
                senderAccountId,
                receiverAccountId,
                transferAmount
        );

        TransferResponse actualResponse =
                new ValidatedCrudRequester<TransferResponse>(
                        senderUserSpec,
                        Endpoint.TRANSFER_WITH_FRAUD_CHECK,
                        ResponseSpecs.requestReturnsOk()
                ).post(transferRequest);

        TransferResponse expectedResponse = TransferResponse.builder()
                .status("VERIFICATION_REQUIRED")
                .message("Additional verification required")
                .amount(transferAmount)
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .fraudRiskScore(0.7)
                .fraudReason("Additional verification required")
                .requiresManualReview(false)
                .requiresVerification(false)
                .build();

        ModelAssertions.assertThatModels(expectedResponse, actualResponse).match();
    }


    @Test
    @FraudCheckMock(
            status = "SUCCESS",
            decision = "APPROVED",
            riskScore = 0.7,
            reason = "Additional verification required",
            requiresManualReview = false,
            additionalVerificationRequired = true
    )
    public void transferRequiresVerificationWhenFraudRequiresAdditionalVerification() {
        RequestSpecification senderUserSpec = createUserSpecForTest();

        int senderAccountId = AccountSteps.createAccount(senderUserSpec);

        AccountSteps.prepareAccountWithSmallBalance(senderUserSpec, senderAccountId);

        RequestSpecification receiverUserSpec = createUserSpecForTest();

        int receiverAccountId = AccountSteps.createAccount(receiverUserSpec);

        double transferAmount = RandomModelGenerator.generateValidDepositAmount();

        TransferRequest transferRequest = fraudCheckTransferRequest(
                senderAccountId,
                receiverAccountId,
                transferAmount
        );

        TransferResponse actualResponse =
                new ValidatedCrudRequester<TransferResponse>(
                        senderUserSpec,
                        Endpoint.TRANSFER_WITH_FRAUD_CHECK,
                        ResponseSpecs.requestReturnsOk()
                ).post(transferRequest);

        TransferResponse expectedResponse = TransferResponse.builder()
                .status("APPROVED")
                .message("Transfer approved and processed immediately")
                .amount(transferAmount)
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .fraudRiskScore(0.7)
                .fraudReason("Additional verification required")
                .requiresManualReview(false)
                .requiresVerification(true)
                .build();

        ModelAssertions.assertThatModels(expectedResponse, actualResponse).match();
    }


    @Test
    @FraudCheckMock(
            behavior = FraudMockBehavior.TIMEOUT
    )
    public void transferRequiresManualReviewWhenFraudCheckTimesOut() {
        RequestSpecification senderUserSpec = createUserSpecForTest();

        int senderAccountId = AccountSteps.createAccount(senderUserSpec);

        AccountSteps.prepareAccountWithSmallBalance(senderUserSpec, senderAccountId);

        RequestSpecification receiverUserSpec = createUserSpecForTest();

        int receiverAccountId = AccountSteps.createAccount(receiverUserSpec);

        double transferAmount = RandomModelGenerator.generateValidDepositAmount();

        TransferRequest transferRequest = fraudCheckTransferRequest(
                senderAccountId,
                receiverAccountId,
                transferAmount
        );

        TransferResponse actualResponse =
                new ValidatedCrudRequester<TransferResponse>(
                        senderUserSpec,
                        Endpoint.TRANSFER_WITH_FRAUD_CHECK,
                        ResponseSpecs.requestReturnsOk()
                ).post(transferRequest);

        TransferResponse expectedResponse = TransferResponse.builder()
                .status("MANUAL_REVIEW_REQUIRED")
                .message("Transfer requires manual review")
                .amount(transferAmount)
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .fraudRiskScore(0.5)
                .fraudReason("External fraud service returned unexpected response")
                .requiresManualReview(true)
                .requiresVerification(false)
                .build();

        ModelAssertions.assertThatModels(expectedResponse, actualResponse).match();
    }


    @Test
    @FraudCheckMock(
            behavior = FraudMockBehavior.CONNECTION_ERROR
    )
    public void transferRequiresManualReviewWhenFraudCheckConnectionFails() {
        RequestSpecification senderUserSpec = createUserSpecForTest();

        int senderAccountId = AccountSteps.createAccount(senderUserSpec);

        AccountSteps.prepareAccountWithSmallBalance(senderUserSpec, senderAccountId);

        RequestSpecification receiverUserSpec = createUserSpecForTest();

        int receiverAccountId = AccountSteps.createAccount(receiverUserSpec);

        double transferAmount = RandomModelGenerator.generateValidDepositAmount();

        TransferRequest transferRequest = fraudCheckTransferRequest(
                senderAccountId,
                receiverAccountId,
                transferAmount
        );

        TransferResponse actualResponse =
                new ValidatedCrudRequester<TransferResponse>(
                        senderUserSpec,
                        Endpoint.TRANSFER_WITH_FRAUD_CHECK,
                        ResponseSpecs.requestReturnsOk()
                ).post(transferRequest);

        TransferResponse expectedResponse = TransferResponse.builder()
                .status("MANUAL_REVIEW_REQUIRED")
                .message("Transfer requires manual review")
                .amount(transferAmount)
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .fraudRiskScore(0.5)
                .fraudReason("Fraud detection service is currently unavailable")
                .requiresManualReview(true)
                .requiresVerification(false)
                .build();

        ModelAssertions.assertThatModels(expectedResponse, actualResponse).match();
    }


    @Test
    @FraudCheckMock(
            behavior = FraudMockBehavior.HTTP_ERROR,
            httpStatus = 500
    )
    public void transferRequiresManualReviewWhenFraudCheckReturnsServerError() {
        RequestSpecification senderUserSpec = createUserSpecForTest();

        int senderAccountId = AccountSteps.createAccount(senderUserSpec);

        AccountSteps.prepareAccountWithSmallBalance(senderUserSpec, senderAccountId);

        RequestSpecification receiverUserSpec = createUserSpecForTest();

        int receiverAccountId = AccountSteps.createAccount(receiverUserSpec);

        double transferAmount = RandomModelGenerator.generateValidDepositAmount();

        TransferRequest transferRequest = fraudCheckTransferRequest(
                senderAccountId,
                receiverAccountId,
                transferAmount
        );

        TransferResponse actualResponse =
                new ValidatedCrudRequester<TransferResponse>(
                        senderUserSpec,
                        Endpoint.TRANSFER_WITH_FRAUD_CHECK,
                        ResponseSpecs.requestReturnsOk()
                ).post(transferRequest);

        TransferResponse expectedResponse = TransferResponse.builder()
                .status("MANUAL_REVIEW_REQUIRED")
                .message("Transfer requires manual review")
                .amount(transferAmount)
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .fraudRiskScore(0.5)
                .fraudReason("Unexpected error during fraud check: 500 Server Error: [no body]")
                .requiresManualReview(true)
                .requiresVerification(false)
                .build();

        ModelAssertions.assertThatModels(expectedResponse, actualResponse).match();
    }
}
