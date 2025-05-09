package ourpkg.attribute;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeRepo extends JpaRepository<Attribute, Integer>{

	List<Attribute> findByCategory2List_Id(Integer category2Id);

}
