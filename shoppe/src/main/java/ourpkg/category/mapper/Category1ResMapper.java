package ourpkg.category.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import ourpkg.category.Category1;
import ourpkg.category.dto.Category1ResDTO;

@Mapper(componentModel = "spring")
public interface Category1ResMapper {

	Category1ResDTO toDto(Category1 entity);
	
	List<Category1ResDTO> toDtoList(List<Category1> entityList);
}
