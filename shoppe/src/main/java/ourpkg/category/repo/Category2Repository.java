package ourpkg.category.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ourpkg.category.Category2;

public interface Category2Repository extends JpaRepository<Category2, Integer> {

	Optional<Category2> findByName(String name);

	boolean existsByName(String name);

	List<Category2> findByCategory1List_Id(Integer category1Id);

	// 確認該 (category1Id, category2Id) 組合是否存在
	boolean existsByIdAndCategory1List_Id(Integer category2Id, Integer category1Id);

	boolean existsByCategory1List_Id(Integer id);
}
