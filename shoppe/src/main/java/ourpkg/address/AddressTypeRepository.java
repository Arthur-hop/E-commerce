package ourpkg.address;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressTypeRepository extends JpaRepository<AddressTypeCorrespond, Integer>{
    Optional<AddressTypeCorrespond> findByName(String name);
    Optional<AddressTypeCorrespond> findById(Integer id);
}
