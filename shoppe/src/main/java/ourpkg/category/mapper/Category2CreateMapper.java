package ourpkg.category.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ourpkg.category.Category2;
import ourpkg.category.dto.Category2CreateDTO;

@Mapper(componentModel = "spring")
public interface Category2CreateMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "category1List", ignore = true)
	@Mapping(target = "attributeList", ignore = true)
	@Mapping(target = "productList", ignore = true)
	Category2 toEntity(Category2CreateDTO dto);
}
