package factories;

import generators.RandomModelGenerator;
import models.ProfileUpdateRequest;

public class ProfileRequestFactory {

    private ProfileRequestFactory() {
    }

    public static ProfileUpdateRequest profileUpdateRequest(String name) {
        return ProfileUpdateRequest.builder()
                .name(name)
                .build();
    }

    public static ProfileUpdateRequest validProfileUpdateRequest() {
        return profileUpdateRequest(RandomModelGenerator.generateValidProfileName());
    }
}
