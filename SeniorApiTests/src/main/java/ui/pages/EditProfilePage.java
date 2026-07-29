package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;

public class EditProfilePage extends BasePage<EditProfilePage>{
    private SelenideElement newNameInput = $(Selectors.byAttribute("placeholder","Enter new name"));
    private SelenideElement saveChangesButton = $$("button").findBy(Condition.text("Save Changes"));
    private SelenideElement homeButton = $$("button").findBy(Condition.text("Home"));
    @Override
    public String url() {
        return "/edit-profile";
    }

    public EditProfilePage changeName(String newName){
        newNameInput.shouldBe(visible).setValue(newName);
        saveChangesButton.shouldBe(visible).shouldBe(enabled).click();

        return this;
    }

    public UserDashboard openUserDashboard(){
        homeButton
                .shouldBe(visible)
                .shouldBe(enabled)
                .click();

        return page(UserDashboard.class);
    }
}
