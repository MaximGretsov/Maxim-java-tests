package specs;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import models.LoginUserRequest;
import request.LoginUserRequester;

import java.util.List;

public class RequestsSpecs {
    private RequestsSpecs(){};

    private static RequestSpecBuilder defaultRequestBuilder(){
        return new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilters(List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()))
                .setBaseUri("http://localhost:4111");
    }

    public static RequestSpecification unauthSpec(){
        return defaultRequestBuilder().build();
    }

    public static RequestSpecification adminSpec(){
        return defaultRequestBuilder()
                .addHeader("Authorization", "Basic YWRtaW46YWRtaW4=")
                .build();
    }

    public static RequestSpecification authAsUserSpec(String username, String password){
        String userAuthHeader =  new LoginUserRequester(
                RequestsSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOk())
                .post(LoginUserRequest.builder().username(username).password(password).build())
                .extract()
                .header("Authorization");
        return defaultRequestBuilder()
                .addHeader("Authorization", userAuthHeader)
                .build();
    }

    public static RequestSpecification brokenAuthSpec(){
        return defaultRequestBuilder()
                .addHeader("Authorization", "brokenToken")
                .build();
    }
}
