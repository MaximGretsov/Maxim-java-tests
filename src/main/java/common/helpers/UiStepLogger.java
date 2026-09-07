package common.helpers;

import com.codeborne.selenide.WebDriverRunner;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.ByteArrayInputStream;

public final class UiStepLogger {

    private UiStepLogger() {
    }

    public static <T> T log(
            String title,
            StepLogger.ThrowableRunnable<T> action
    ) {
        return Allure.step(title, () -> {
            try {
                return action.run();
            } finally {
                attachScreenshot();
            }
        });
    }

    public static void log(
            String title,
            StepLogger.ThrowableVoidRunnable action
    ) {
        Allure.step(title, () -> {
            try {
                action.run();
            } finally {
                attachScreenshot();
            }

            return null;
        });
    }

    private static void attachScreenshot() {
        if (!WebDriverRunner.hasWebDriverStarted()) {
            return;
        }

        try {
            byte[] screenshot =
                    ((TakesScreenshot) WebDriverRunner.getWebDriver())
                            .getScreenshotAs(OutputType.BYTES);

            Allure.addAttachment(
                    "Page screenshot",
                    "image/png",
                    new ByteArrayInputStream(screenshot),
                    ".png"
            );
        } catch (RuntimeException exception) {
            System.out.println(
                    "Failed to attach screenshot: " + exception.getMessage()
            );
        }
    }
}