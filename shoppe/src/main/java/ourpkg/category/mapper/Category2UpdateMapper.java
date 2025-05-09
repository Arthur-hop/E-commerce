package ourpkg.category.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import ourpkg.category.Category2;
import ourpkg.category.dto.Category2UpdateDTO;

@Mapper(componentModel = "spring")
public interface Category2UpdateMapper {
	
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "category1List", ignore = true)
	@Mapping(target = "attributeList", ignore = true)
	@Mapping(target = "productList", ignore = true)
	Category2 toEntity(@MappingTarget Category2 category2, Category2UpdateDTO dto);	
}
