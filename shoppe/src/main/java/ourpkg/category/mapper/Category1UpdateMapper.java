package ourpkg.category.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import ourpkg.category.Category1;
import ourpkg.category.dto.Category1UpdateDTO;

@Mapper(componentModel = "spring")
public interface Category1UpdateMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "category2List", ignore = true)
	@Mapping(target = "productList", ignore = true)
	Category1 toEntity(@MappingTarget Category1 category1, Category1UpdateDTO dto);
}
