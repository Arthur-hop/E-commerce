package ourpkg.category.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ourpkg.category.Category1;

public interface Category1Repository extends JpaRepository<Category1, Integer> {

	Optional<Category1> findByName(String name);

	boolean existsByName(String name);

}
