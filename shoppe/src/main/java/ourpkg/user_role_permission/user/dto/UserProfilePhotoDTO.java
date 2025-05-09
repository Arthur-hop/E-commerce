package ourpkg.user_role_permission.user.dto;

public class UserProfilePhotoDTO {
	
	private String profilePhotoUrl;

    public UserProfilePhotoDTO(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

}
