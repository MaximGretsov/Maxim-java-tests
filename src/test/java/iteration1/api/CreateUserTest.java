package iteration1.api;

import api.dao.UserDao;
import api.dao.comparison.DaoAndModelAssertions;
import api.generators.RandomModelGenerator;
import api.requests.steps.DataBaseSteps;
import iteration2.api.BaseTest;
import api.models.CreateUserRequest;
import api.models.CreateUserResponse;
import api.models.comparison.ModelAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.requests.skeleton.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateUserTest extends BaseTest {

    @Test
    public void adminCanCreateUserWithCorrectData() {
        CreateUserRequest createUserRequest =
                RandomModelGenerator.generate(CreateUserRequest.class);

        CreateUserResponse createUserResponse =
                new ValidatedCrudRequester<CreateUserResponse>(
                        RequestSpecs.adminSpec(),
                        Endpoint.ADMIN_USER,
                        ResponseSpecs.entityWasCreated()
                )
                        .post(createUserRequest);

        registerUserForDeletion(createUserResponse.getId());

        ModelAssertions
                .assertThatModels(createUserRequest, createUserResponse)
                .match();

        UserDao userDao = DataBaseSteps.getUserByUsername(createUserResponse.getUsername());

        DaoAndModelAssertions
                .assertThat(createUserResponse, userDao)
                .match();
    }

    public static Stream<Arguments> userInvalidData(){
        return Stream.of(
                // username field validation
                Arguments.of("   ", "Password33$", "USER", "username", List.of("Username cannot be blank", "Username must contain only letters, digits, dashes, underscores, and dots")),
                Arguments.of("ab", "Password33$", "USER", "username", List.of("Username must be between 3 and 15 characters")),
                Arguments.of("abc$", "Password33$", "USER", "username", List.of("Username must contain only letters, digits, dashes, underscores, and dots")),
                Arguments.of("abc%", "Password33$", "USER", "username", List.of("Username must contain only letters, digits, dashes, underscores, and dots"))
        );
    }

    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void adminCanNotCreateUserWithWrongData(String username, String password, String role,
                                                   String errorKey, List<String> errorValues){
            CreateUserRequest createUserRequest = CreateUserRequest.builder()
                    .username(username)
                    .password(password)
                    .role(role)
                    .build();

            new CrudRequester(
                    RequestSpecs.adminSpec(),
                    Endpoint.ADMIN_USER,
                    ResponseSpecs.requestReturnsBadRequest(errorKey, errorValues)
            )
                    .post(createUserRequest);

            UserDao userDao = DataBaseSteps.getUserByUsername(username);

            // если вдруг бек все-таки создаст пользователя и ошибки не будет - мы удалим пользователя
            if (userDao != null) {
                registerUserForDeletion(userDao.getId());
            }

            assertThat(userDao)
                    .as("User with invalid data should not be created in database")
                    .isNull();
    }
}
