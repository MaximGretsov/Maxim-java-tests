package ui.pages;

import lombok.Getter;

@Getter
public enum BankAlert {
    USER_CREATED_SUCCESSFULLY("✅ User created successfully!"),
    USERNAME_MUST_BE_BETWEEN_3_AND_15_CHARACTERS("❌ Failed to create user:\n\n" +
                    "• username: Username must be between 3 and 15 characters"),
    NEW_ACCOUNT_CREATED("✅ New Account Created! Account Number: "),
    GOOD_DEPOSIT("✅ Successfully deposited $%s to account %s!"),
    BAD_DEPOSIT("❌ Please enter a valid amount."),
    PROFILE_UPDATED_SUCCESSFULLY("✅ Name updated successfully!"),
    ENTER_VALID_NAME("Name must contain two words with letters only"),
    GOOD_TRANSFER( "✅ Successfully transferred $%s to account %s!"),
    AMOUNT_MUST_BE_MORE_THAN_MINIMUM("❌ Error: Invalid transfer: insufficient funds or invalid accounts"),
    REPEAT_TRANSFER_SUCCESS("✅ Transfer of $%s successful from Account %s to %s!"),
    NO_MATCHING_USERS_FOUND("❌ No matching users found."),
    REPEAT_TRANSFER_FAILED("❌ Transfer failed: Please try again.");

    private final String message;

    BankAlert(String message) {
        this.message = message;
    }

    public String format(Object... values) {
        return String.format(message, values);
    }
}
