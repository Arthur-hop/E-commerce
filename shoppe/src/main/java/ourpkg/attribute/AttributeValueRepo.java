package ourpkg.attribute;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeValueRepo extends JpaRepository<AttributeValue, Integer> {

	List<AttributeValue> findByAttribute_Id(Integer attributeId);

}
