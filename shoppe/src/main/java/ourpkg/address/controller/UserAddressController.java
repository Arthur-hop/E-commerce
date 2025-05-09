package ourpkg.address.controller;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ourpkg.address.AddressTypeCorrespond;
import ourpkg.address.AddressTypeRepository;
import ourpkg.address.CVSAddressDto;
import ourpkg.address.UserAddress;
import ourpkg.address.UserAddressRepository;
import ourpkg.address.dto.UserAddressDTO;
import ourpkg.address.service.UserAddressService;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;

@RestController
@RequestMapping("/api/user/address")
public class UserAddressController {
	
	@Autowired
	private UserAddressService userAddressService;
	
	@Autowired
	private UserAddressRepository userAddressRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private AddressTypeRepository addressTypeRepository;

	
	// 透過 userId 取得所有地址
	@GetMapping("/{userId}")
	public ResponseEntity<List<UserAddress>> getUserAddresses(@PathVariable Integer userId) {
		List<UserAddress> addresses = userAddressService.getAddressesByUserId(userId);
		return ResponseEntity.ok(addresses);
	}

	// 透過 userId 和 addressId 更新地址
	@PutMapping("/{userId}/{addressId}")
	public ResponseEntity<String> updateUserAddress(@PathVariable Integer userId, @PathVariable Integer addressId,
			@RequestBody UserAddress updatedAddress) {
		try {
			userAddressService.updateUserAddress(userId, addressId, updatedAddress);
			return ResponseEntity.ok("地址更新成功");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("更新地址失敗");
		}
	}
	
//	 透過 addressId 更新地址
		@PutMapping("/{addressId}")
		public ResponseEntity<String> updateUserAddress2(@PathVariable Integer addressId,
				@RequestBody UserAddress updatedAddress) {
			try {
				userAddressService.updateUserAddress2(addressId, updatedAddress);
				return ResponseEntity.ok("地址更新成功");
			} catch (Exception e) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("更新地址失敗");
			}
		}

	@GetMapping("/{userId}/type/{addressTypeId}")
	public ResponseEntity<List<UserAddressDTO>> getAddressByUserIdAndType(@PathVariable Integer userId,
			@PathVariable Integer addressTypeId) {

		List<UserAddress> addresses = userAddressService.getAddressesByUserIdAndType(userId, addressTypeId);

//		if (addresses.isEmpty()) {
//			return ResponseEntity.notFound().build();
//		}

		List<UserAddressDTO> addressDTOs = addresses.stream()
				.map(address -> new UserAddressDTO(address.getCity(), address.getDistrict(), address.getZipCode(),
						address.getStreetEtc(), address.getRecipientName(), address.getRecipientPhone(), address.getUserAddressId()))
				.collect(Collectors.toList());

		return ResponseEntity.ok(addressDTOs);
	}
	
	/**
     * 新增地址
     */
	@PostMapping("/{userId}/create-home")
	public ResponseEntity<?> createAddress(@PathVariable Integer userId, @RequestBody UserAddress addressRequest) {
	    Optional<User> userOpt = userRepository.findById(userId);
	    if (userOpt.isEmpty()) {
	        return ResponseEntity.badRequest().body("User not found");
	    }

	    User user = userOpt.get();

	    // 🔒 強制 addressType 為宅配（typeId = 1）
	    Optional<AddressTypeCorrespond> typeOpt = addressTypeRepository.findById(1);
	    if (typeOpt.isEmpty()) {
	        return ResponseEntity.badRequest().body("找不到對應的地址類型");
	    }
	    AddressTypeCorrespond type = typeOpt.get();

	    UserAddress address = new UserAddress();
	    address.setUser(user);
	    address.setAddressTypeCorrespond(type);

	    // 查詢目前該 user 該類型的所有地址
	    List<UserAddress> existingAddresses = userAddressRepository
	            .findByUserUserIdAndAddressTypeCorrespondId(user.getUserId(), type.getId());

	    // 判斷是否第一筆 or 手動設為預設
	    if (Boolean.TRUE.equals(addressRequest.getIsDefault()) || existingAddresses.isEmpty()) {
	        address.setIsDefault(true);

	        // 取消其他同類型地址的預設
	        for (UserAddress addr : existingAddresses) {
	            if (Boolean.TRUE.equals(addr.getIsDefault())) {
	                addr.setIsDefault(false);
	                userAddressRepository.save(addr);
	            }
	        }
	    } else {
	        address.setIsDefault(false);
	    }

	    // 🔥 拆解地址字串為 city / district / street
	    if (addressRequest.getStreetEtc() != null) {
	        String[] parts = addressRequest.getStreetEtc().split(" ");
	        if (parts.length >= 3) {
	            address.setCity(parts[0]);
	            address.setDistrict(parts[1]);
	            address.setStreetEtc(String.join(" ", Arrays.copyOfRange(parts, 2, parts.length)));
	        } else {
	            return ResponseEntity.badRequest().body("地址格式錯誤，請包含縣市、區域與詳細地址");
	        }
	    } else {
	        return ResponseEntity.badRequest().body("地址不可為空");
	    }

	    // 其他欄位
	    address.setZipCode(addressRequest.getZipCode());
	    address.setRecipientName(addressRequest.getRecipientName());
	    address.setRecipientPhone(addressRequest.getRecipientPhone());
	    address.setCreatedAt(new Date());
	    address.setUpdatedAt(new Date());

	    UserAddress saved = userAddressRepository.save(address);
	    return ResponseEntity.ok(saved);
	}
	
	@PostMapping("/{userId}/create-cvs")
	public ResponseEntity<?> createCvsAddress(@PathVariable Integer userId, @RequestBody UserAddress addressRequest) {
	    Optional<User> userOpt = userRepository.findById(userId);
	    if (userOpt.isEmpty()) {
	        return ResponseEntity.badRequest().body("User not found");
	    }

	    User user = userOpt.get();

	    // 🔒 強制 addressType 為超商（typeId = 2）
	    Optional<AddressTypeCorrespond> typeOpt = addressTypeRepository.findById(2);
	    if (typeOpt.isEmpty()) {
	        return ResponseEntity.badRequest().body("找不到對應的地址類型");
	    }
	    AddressTypeCorrespond type = typeOpt.get();

	    UserAddress address = new UserAddress();
	    address.setUser(user);
	    address.setAddressTypeCorrespond(type);

	    // 查詢目前該 user 該類型的所有地址
	    List<UserAddress> existingAddresses = userAddressRepository
	            .findByUserUserIdAndAddressTypeCorrespondId(user.getUserId(), type.getId());

	    // 判斷是否第一筆 or 手動設為預設
	    if (Boolean.TRUE.equals(addressRequest.getIsDefault()) || existingAddresses.isEmpty()) {
	        address.setIsDefault(true);

	        // 取消其他同類型地址的預設
	        for (UserAddress addr : existingAddresses) {
	            if (Boolean.TRUE.equals(addr.getIsDefault())) {
	                addr.setIsDefault(false);
	                userAddressRepository.save(addr);
	            }
	        }
	    } else {
	        address.setIsDefault(false);
	    }

	    // 🔥 必填欄位驗證
	    if (addressRequest.getStreetEtc() == null || addressRequest.getStreetEtc().isBlank()) {
	        return ResponseEntity.badRequest().body("請提供完整門市地址");
	    }
	    if (addressRequest.getRecipientName() == null || addressRequest.getRecipientPhone() == null) {
	        return ResponseEntity.badRequest().body("請提供收件人姓名與電話");
	    }

	    // 超商地址不需要解析 city/district，直接存入 streetEtc
	    address.setStreetEtc(addressRequest.getStreetEtc());
	    address.setCity(addressRequest.getCity());
	    address.setDistrict(addressRequest.getDistrict());

	    address.setZipCode(addressRequest.getZipCode());
	    address.setRecipientName(addressRequest.getRecipientName());
	    address.setRecipientPhone(addressRequest.getRecipientPhone());
	    address.setCreatedAt(new Date());
	    address.setUpdatedAt(new Date());

	    UserAddress saved = userAddressRepository.save(address);
	    return ResponseEntity.ok(saved);
	}







    /**
     * 刪除地址
     */
    @DeleteMapping("/{userId}/{addressId}")
    public ResponseEntity<?> deleteAddress(@PathVariable Integer userId, @PathVariable Integer addressId) {
        Optional<UserAddress> addressOpt = userAddressRepository.findById(addressId);
        if (addressOpt.isEmpty() || !addressOpt.get().getUser().getUserId().equals(userId)) {
            return ResponseEntity.badRequest().body("Address not found or user mismatch");
        }

        userAddressRepository.deleteById(addressId);
        return ResponseEntity.ok("Address deleted successfully");
    }
    
    @PutMapping("/{userId}/set-default/{addressId}")
    public ResponseEntity<?> setDefaultAddress(
            @PathVariable Integer userId,
            @PathVariable Integer addressId) {

        Optional<UserAddress> targetOpt = userAddressRepository.findById(addressId);
        if (targetOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("找不到該地址");
        }

        UserAddress target = targetOpt.get();

        // 只允許修改屬於該 user 的地址
        if (!target.getUser().getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("用戶不符");
        }

        // 找出所有同類型地址
        List<UserAddress> allSameType = userAddressRepository.findByUserUserIdAndAddressTypeCorrespondId(
                userId, target.getAddressTypeCorrespond().getId());

        // 全部設為 false，指定的設為 true
        for (UserAddress addr : allSameType) {
            addr.setIsDefault(addr.getUserAddressId().equals(addressId));
            userAddressRepository.save(addr);
        }

        return ResponseEntity.ok("已更新為預設地址");
    }

    
    //---------------CVS API 好難喔二二---------------------------

	private static final String MERCHANT_ID = "2000132"; // 測試商店
	private static final String LOGISTICS_TYPE = "CVS"; // 超商
	private static final String LOGISTICS_SUB_TYPE = "UNIMART"; // 7-11
	private static final String IS_COLLECTION = "N"; // 是否代收貨款 (N=否, Y=是)
	private static final String SERVER_REPLY_URL = "https://yourbackend.com/api/user-address/ecpay-callback"; // **你的後端
																												// API**
	private static final String EC_PAY_URL = "https://logistics-stage.ecpay.com.tw/Express/map";

	/**
	 * 產生 7-11 門市選擇的 URL
	 */
	@GetMapping("/store-selection-url")
	public ResponseEntity<String> getStoreSelectionURL() {
		String url = EC_PAY_URL + "?MerchantID=" + MERCHANT_ID + "&LogisticsType=" + LOGISTICS_TYPE
				+ "&LogisticsSubType=" + LOGISTICS_SUB_TYPE + "&IsCollection=" + IS_COLLECTION + "&ServerReplyURL="
				+ SERVER_REPLY_URL;
		return ResponseEntity.ok(url);
	}

	/**
	 * 綠界回傳門市選擇結果
	 */
	@PostMapping("/ecpay-callback")
	public ResponseEntity<String> handleECPayCallback(@RequestParam Map<String, String> params) {
		System.out.println("📩 收到綠界回傳：" + params);

		// 解析門市資訊
		String storeID = params.get("StoreID"); // 門市代號
		String storeName = params.get("StoreName"); // 門市名稱
		String storeAddress = params.get("StoreAddress"); // 門市地址

		// 這裡可以選擇將門市資訊 **更新到對應的 UserAddress**
		System.out.println("🏪 門市資訊： " + storeName + " (" + storeID + ") 地址：" + storeAddress);

		return ResponseEntity.ok("OK");
	}
	
	 @PostMapping("/cvs")
	    public ResponseEntity<String> saveCvsAddress(@RequestBody CVSAddressDto addressDto) {
	        userAddressService.saveCvsAddress(addressDto);
	        return ResponseEntity.ok("儲存成功");
	    }
}
