package request;

import io.restassured.common.mapper.TypeRef;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.AccountResponse;

import java.util.List;

import static io.restassured.RestAssured.given;


public class CustomerAccountsRequester extends GetRequester{
    public CustomerAccountsRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    public ValidatableResponse get() {
        return given()
                .spec(requestSpecification)
                .get("/api/v1/customer/accounts")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    // Получение списка аккаунтов как Java-объектов
    public List<AccountResponse> getAccounts() {
        return get()
                .extract()
                .as(new TypeRef<List<AccountResponse>>() {});
    }
}
