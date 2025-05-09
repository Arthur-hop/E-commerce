package ourpkg.cart;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {

    // ✅ 查詢購物車內某個 SKU
    Optional<Cart> findByUser_UserIdAndSku_skuId(Integer userId, Integer skuId);

    // ✅ 查詢使用者購物車內所有商品
    List<Cart> findByUser_UserId(Integer userId);

	void deleteBySku_SkuId(Integer id);

}
