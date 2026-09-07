package iteration1.api;


import api.dao.UserDao;
import api.dao.comparison.DaoAndModelAssertions;
import api.models.LoginUserResponse;
import api.requests.steps.DataBaseSteps;
import iteration2.api.BaseTest;
import api.models.CreateUserRequest;
import api.models.LoginUserRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

public class LoginUserTest extends BaseTest {
    @Test
    public void adminCanGenerateAuthTokenTest() {
        LoginUserRequest loginAdminRequest = LoginUserRequest.builder()
                .username("admin")
                .password("admin")
                .build();

        LoginUserResponse loginResponse =
                new CrudRequester(
                        RequestSpecs.unauthSpec(),
                        Endpoint.LOGIN,
                        ResponseSpecs.requestReturnsOk()
                )
                        .post(loginAdminRequest)
                        .header("Authorization", Matchers.notNullValue())
                        .extract()
                        .as(LoginUserResponse.class);

        UserDao adminDao = DataBaseSteps.getUserByUsername(loginAdminRequest.getUsername());

        DaoAndModelAssertions
                .assertThat(loginResponse, adminDao)
                .match();
    }

    @Test
    public void userCanGenerateAuthTokenTest() {
        CreateUserRequest userRequest = createUserForTest();

        LoginUserRequest loginUserRequest = LoginUserRequest.builder()
                .username(userRequest.getUsername())
                .password(userRequest.getPassword())
                .build();

        LoginUserResponse loginResponse =
                new CrudRequester(
                        RequestSpecs.unauthSpec(),
                        Endpoint.LOGIN,
                        ResponseSpecs.requestReturnsOk()
                )
                        .post(loginUserRequest)
                        .header("Authorization", Matchers.notNullValue())
                        .extract()
                        .as(LoginUserResponse.class);

        UserDao userDao = DataBaseSteps.getUserByUsername(userRequest.getUsername());

        DaoAndModelAssertions
                .assertThat(loginResponse, userDao)
                .match();
    }
}
