package ourpkg.address.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ourpkg.address.AddressTypeCorrespond;
import ourpkg.address.AddressTypeRepository;
import ourpkg.address.CVSAddressDto;
import ourpkg.address.UserAddress;
import ourpkg.address.UserAddressRepository;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;

@Service
@Transactional
public class UserAddressService {
	 @Autowired
	    private UserAddressRepository userAddressRepository;

	    @Autowired
	    private UserRepository userRepository;
	    
	    @Autowired
	    private AddressTypeRepository addressTypeRepository;


	    // 透過 user_id 取得該用戶的所有地址
	    public List<UserAddress> getAddressesByUserId(Integer userId) {
	        return userAddressRepository.findByUserUserId(userId);
	    }

	    // 更新 UserAddress
	    public UserAddress updateUserAddress(Integer userId, Integer addressId, UserAddress updatedAddress) {
	        Optional<UserAddress> optionalAddress = userAddressRepository.findByUserUserIdAndUserAddressId(userId, addressId);
	        
	        if (optionalAddress.isPresent()) {
	            UserAddress existingAddress = optionalAddress.get();

	            // 更新字段
	            existingAddress.setCity(updatedAddress.getCity());
	            existingAddress.setDistrict(updatedAddress.getDistrict());
	            existingAddress.setStreetEtc(updatedAddress.getStreetEtc());
	            existingAddress.setZipCode(updatedAddress.getZipCode());
	            existingAddress.setRecipientName(updatedAddress.getRecipientName());
	            existingAddress.setRecipientPhone(updatedAddress.getRecipientPhone());

	            return userAddressRepository.save(existingAddress);
	        } else {
	            throw new RuntimeException("Address not found for userId: " + userId + " and addressId: " + addressId);
	        }
	    }
	    
	    public UserAddress updateUserAddress2(Integer addressId, UserAddress updatedAddress) {
	        Optional<UserAddress> optionalAddress = userAddressRepository.findByUserAddressId(addressId);

	        if (optionalAddress.isPresent()) {
	            UserAddress existingAddress = optionalAddress.get();

	            // 🔥 拆解 address 字串（台北市 信義區 忠孝東路100號）
	            String fullAddress = updatedAddress.getStreetEtc();
	            if (fullAddress != null && !fullAddress.isEmpty()) {
	                String[] parts = fullAddress.split(" ");
	                if (parts.length >= 3) {
	                    existingAddress.setCity(parts[0]);
	                    existingAddress.setDistrict(parts[1]);
	                    existingAddress.setStreetEtc(String.join(" ", Arrays.copyOfRange(parts, 2, parts.length)));
	                } else {
	                    throw new IllegalArgumentException("地址格式錯誤，應包含縣市、區域與詳細地址");
	                }
	            }

	            // ✅ 更新收件人資訊
	            existingAddress.setRecipientName(updatedAddress.getRecipientName());
	            existingAddress.setRecipientPhone(updatedAddress.getRecipientPhone());

	            return userAddressRepository.save(existingAddress);
	        } else {
	            throw new RuntimeException("Address not found for addressId: " + addressId);
	        }
	    }

	    
	    public List<UserAddress> getAddressesByUserIdAndType(Integer userId, Integer addressTypeId) {
	        return userAddressRepository.findByUserUserIdAndAddressTypeCorrespondId(userId, addressTypeId);
	    }
	    
	    public void saveCvsAddress(CVSAddressDto dto) {
	        User user = userRepository.findById(dto.getUserId())
	                .orElseThrow(() -> new RuntimeException("使用者不存在"));

	        AddressTypeCorrespond addressType = addressTypeRepository.findById(dto.getAddressTypeId())
	                .orElseThrow(() -> new RuntimeException("地址類型不存在"));

	        UserAddress address = new UserAddress();
	        address.setUser(user);
	        address.setAddressTypeCorrespond(addressType);
	        address.setIsDefault(dto.getIsDefault());
	        address.setCity(dto.getCity());
	        address.setDistrict(dto.getDistrict());
	        address.setZipCode(dto.getZipCode());
	        address.setStreetEtc(dto.getStreetEtc());

	        userAddressRepository.save(address);
	    }
	
}
