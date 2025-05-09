package ourpkg.user_role_permission;

public class AdminPhotoUtil {
    
    /**
     * 管理員默認頭像路徑
     */
    public static final String DEFAULT_ADMIN_PHOTO = "/uploads/AdminDefault.png";
    
    /**
     * 獲取頭像URL，如果為空則返回默認頭像
     * 
     * @param photoUrl 用戶的頭像URL
     * @return 有效的頭像URL
     */
    public static String getValidPhotoUrl(String photoUrl) {
        return (photoUrl != null && !photoUrl.trim().isEmpty()) ? photoUrl : DEFAULT_ADMIN_PHOTO;
    }
    
    /**
     * 檢查是否為默認頭像
     * 
     * @param photoUrl 頭像URL
     * @return 是否為默認頭像
     */
    public static boolean isDefaultPhoto(String photoUrl) {
        return photoUrl == null || photoUrl.trim().isEmpty() || DEFAULT_ADMIN_PHOTO.equals(photoUrl);
    }
}