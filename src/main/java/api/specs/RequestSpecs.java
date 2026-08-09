package api.specs;

import api.configs.Config;
import com.github.viclovsky.swagger.coverage.SwaggerCoverageRestAssured;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import api.models.LoginUserRequest;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Contains reusable request specifications for API tests.
public class RequestSpecs {
    private static Map<String,String> authHeaders = new HashMap<>(Map.of("admin", "Basic YWRtaW46YWRtaW4="));
    private RequestSpecs(){};

    private static RequestSpecBuilder defaultRequestBuilder(){
        return new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilters(List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter(), new AllureRestAssured(), new SwaggerCoverageRestAssured()))
                .setBaseUri(Config.getProperty("apiBaseUrl"));
    }

    public static RequestSpecification unauthSpec(){
        return defaultRequestBuilder().build();
    }

    public static RequestSpecification adminSpec(){
        return defaultRequestBuilder()
                .addHeader("Authorization", "Basic YWRtaW46YWRtaW4=")
                .build();
    }

    public static RequestSpecification authAsUser(String username, String password){
        return defaultRequestBuilder()
                .addHeader("Authorization", getUserAuthHeader(username,password))
                .build();
    }

    public static RequestSpecification brokenAuthSpec(){
        return defaultRequestBuilder()
                .addHeader("Authorization", "brokenToken")
                .build();
    }

    public static String getUserAuthHeader(String username, String password){
        String userAuthHeader;
        if (!authHeaders.containsKey(username)) {
            userAuthHeader = new CrudRequester(
                    RequestSpecs.unauthSpec(),
                    Endpoint.LOGIN,
                    ResponseSpecs.requestReturnsOk())
                    .post(LoginUserRequest.builder().username(username).password(password).build())
                    .extract()
                    .header("Authorization");
            authHeaders.put(username, userAuthHeader);
        } else {
            userAuthHeader = authHeaders.get(username);
        }

        return userAuthHeader;
    }
}
