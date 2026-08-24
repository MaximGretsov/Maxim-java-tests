package common.extentions;

import api.generators.RandomModelGenerator;
import api.models.CreateUserRequest;
import api.models.CreateUserResponse;
import api.requests.steps.AdminSteps;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import ui.pages.BasePage;

import java.util.ArrayList;
import java.util.List;

public class UserSessionExtension
        implements BeforeEachCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(UserSessionExtension.class);

    private static final String USER_IDS = "created-user-ids";

    @Override
    public void beforeEach(ExtensionContext context) {
        UserSession annotation = context
                .getRequiredTestMethod()
                .getAnnotation(UserSession.class);

        if (annotation == null) {
            return;
        }

        SessionStorage.clear();

        List<CreateUserRequest> users = new ArrayList<>();
        List<Integer> userIds = new ArrayList<>();

        for (int i = 0; i < annotation.value(); i++) {
            CreateUserRequest userRequest =
                    RandomModelGenerator.generate(CreateUserRequest.class);

            CreateUserResponse userResponse =
                    AdminSteps.createUserAndGetResponse(userRequest);

            users.add(userRequest);
            userIds.add(Math.toIntExact(userResponse.getId()));
        }

        context.getStore(NAMESPACE).put(USER_IDS, userIds);

        SessionStorage.addUsers(users);

        BasePage.authAsUser(
                SessionStorage.getUser(annotation.auth())
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public void afterEach(ExtensionContext context) {
        List<Integer> userIds = context
                .getStore(NAMESPACE)
                .remove(USER_IDS, List.class);

        try {
            if (userIds != null) {
                for (Integer userId : userIds) {
                    try {
                        AdminSteps.deleteUser(userId);
                    } catch (Exception exception) {
                        System.err.printf(
                                "Не удалось удалить пользователя %d: %s%n",
                                userId,
                                exception.getMessage()
                        );
                    }
                }
            }
        } finally {
            SessionStorage.clear();
        }
    }
}
