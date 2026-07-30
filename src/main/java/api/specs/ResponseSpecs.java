package api.specs;

import api.configs.Config;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import api.requests.skeleton.Endpoint;

import java.util.List;

public class ResponseSpecs {
    private ResponseSpecs(){};

    private static final String DEPOSIT_AMOUNT_LESS_THAN_MIN = "Deposit amount must be at least 0.01";
    private static final String DEPOSIT_AMOUNT_MORE_THAN_MAX = "Deposit amount cannot exceed 5000";
    private static final String UNAUTHORIZED_ACCESS_TO_ACCOUNT = "Unauthorized access to account";
    private static final int INTERNAL_SERVER_ERROR_STATUS = 500;
    private static final String INTERNAL_SERVER_ERROR_TEXT = "Internal Server Error";
    private static final String TRANSFER_AMOUNT_LESS_THAN_MIN = "Transfer amount must be at least 0.01";
    private static final String TRANSFER_AMOUNT_MORE_THAN_MAX = "Transfer amount cannot exceed 10000";
    private static final String INVALID_TRANSFER = "Invalid transfer: insufficient funds or invalid accounts";
    private static final String PROFILE_NAME_VALIDATION_MESSAGE = "Name must contain two words with letters only";
    public static final String TRANSFER_SUCCESS_MESSAGE = "Transfer successful";
    public static final String PROFILE_UPDATE_SUCCESS_MESSAGE = "Profile updated successfully";

    private static ResponseSpecBuilder defaultResponseBuilder(){
        return new ResponseSpecBuilder();
    }

    // 201
    public static ResponseSpecification entityWasCreated(){
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_CREATED)
                .build();
    }

    // 200
    public static ResponseSpecification requestReturnsOk(){
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .build();
    }

    // успешный депозит
    public static ResponseSpecification successfulDepositResponse(int accountId, float expectedBalance){
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .expectBody("id", Matchers.equalTo(accountId))
                .expectBody("accountNumber", Matchers.equalTo("ACC" + accountId))
                .expectBody("balance", Matchers.comparesEqualTo(expectedBalance))
                .expectBody("transactions", Matchers.notNullValue())
                .build();
    }

    // успешный трансфер
    public static ResponseSpecification successfulTransferResponse(float expectedAmount,
                                                                   int senderAccountId,
                                                                   int receiverAccountId) {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .expectBody("amount", Matchers.equalTo(expectedAmount))
                .expectBody("receiverAccountId", Matchers.equalTo(receiverAccountId))
                .expectBody("senderAccountId", Matchers.equalTo(senderAccountId))
                .expectBody("message", Matchers.equalTo(TRANSFER_SUCCESS_MESSAGE))
                .build();
    }

    // успешное изменение профиля
    public static ResponseSpecification successfulProfileUpdateResponse(String expectedName){
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .expectBody(
                        "message",
                        Matchers.equalTo(PROFILE_UPDATE_SUCCESS_MESSAGE)
                )
                .expectBody("customer.name", Matchers.equalTo(expectedName))
                .build();
    }

    // 400 для ошибок с ключом
    public static ResponseSpecification requestReturnsBadRequest(String errorKey, List<String> errorValues){
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(errorKey,Matchers.containsInAnyOrder(errorValues.toArray()))
                .build();
    }

    // 400 для ошибок без ключа
    public static ResponseSpecification requestReturnsBadRequestWithText(String errorText){
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(Matchers.equalTo(errorText))
                .build();
    }

    // 403
    public static ResponseSpecification unauthorizedAccessToAccount(){
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_FORBIDDEN)
                .expectBody(Matchers.equalTo(UNAUTHORIZED_ACCESS_TO_ACCOUNT))
                .build();
    }

    // 401
    public static ResponseSpecification unauthorized() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_UNAUTHORIZED)
                .build();
    }

    // 500 (ответ приходит объектом)
    public static ResponseSpecification internalServerErrorForEndpoint(Endpoint endpoint) {
        String expectedPath = Config.getProperty("apiVersion") + endpoint.getUrl();

        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .expectBody("status", Matchers.equalTo(INTERNAL_SERVER_ERROR_STATUS))
                .expectBody("error", Matchers.equalTo(INTERNAL_SERVER_ERROR_TEXT))
                .expectBody("path", Matchers.equalTo(expectedPath))
                .build();
    }

    // 400 если депозит меньше минимума
    public static ResponseSpecification depositAmountLessThanMin() {
        return requestReturnsBadRequestWithText(DEPOSIT_AMOUNT_LESS_THAN_MIN);
    }

    // 400 если депозит больше минимума
    public static ResponseSpecification depositAmountMoreThanMax() {
        return requestReturnsBadRequestWithText(DEPOSIT_AMOUNT_MORE_THAN_MAX);
    }

    // 400 если трансфер меньше минимума
    public static ResponseSpecification transferAmountLessThanMin() {
        return requestReturnsBadRequestWithText(TRANSFER_AMOUNT_LESS_THAN_MIN);
    }

    // 400 если трансфер больше минимума
    public static ResponseSpecification transferAmountMoreThanMax() {
        return requestReturnsBadRequestWithText(TRANSFER_AMOUNT_MORE_THAN_MAX);
    }

    // 400 при невалдином трансфере
    public static ResponseSpecification invalidTransfer() {
        return requestReturnsBadRequestWithText(INVALID_TRANSFER);
    }

    // 400 при некорректном имени
    public static ResponseSpecification profileNameValidationError() {
        return requestReturnsBadRequestWithText(PROFILE_NAME_VALIDATION_MESSAGE);
    }
}
