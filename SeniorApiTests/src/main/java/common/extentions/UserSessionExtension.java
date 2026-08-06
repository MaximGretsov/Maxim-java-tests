package common.extentions;

import api.models.CreateUserRequest;
import api.requests.steps.AdminSteps;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import ui.pages.BasePage;

import java.util.LinkedList;
import java.util.List;

public class UserSessionExtension implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        // Шаг 1: проверка есть ли у теста анотация UserSession
        UserSession annotation = extensionContext.getRequiredTestMethod().getAnnotation(UserSession.class);
        if (annotation != null){ // Шаг 2: если есть, то добавляем в локал сторадж токен админа
            int userCount = annotation.value();

            SessionStorage.clear();

            List<CreateUserRequest> users = new LinkedList<>();

            for (int i = 0; i < userCount; i++){
                CreateUserRequest user = AdminSteps.createUser();
                users.add(user);
            }

            SessionStorage.addUsers(users);

            int authAsUser = annotation.auth();

            BasePage.authAsUser(SessionStorage.getUser(authAsUser));
        }
    }
}
