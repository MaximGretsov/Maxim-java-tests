package iteration1.api;

import api.dao.AccountDao;
import api.dao.comparison.DaoAndModelAssertions;
import api.models.CreateAccountResponse;
import api.models.CreateUserRequest;
import api.requests.skeleton.requesters.ValidatedCrudRequester;
import api.requests.steps.DataBaseSteps;
import iteration2.api.BaseTest;
import org.junit.jupiter.api.Test;
import api.requests.skeleton.Endpoint;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

public class CreateAccountTest extends BaseTest {

    @Test
    public void userCanCreateAccountTest(){
        CreateUserRequest userRequest = createUserForTest();

        CreateAccountResponse createAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>
        (RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post(null);

        AccountDao accountDao = DataBaseSteps.getAccountByAccountNumber(createAccountResponse.getAccountNumber());

        DaoAndModelAssertions.assertThat(createAccountResponse, accountDao).match();
    }
}
