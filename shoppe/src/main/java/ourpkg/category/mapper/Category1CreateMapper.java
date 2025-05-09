package ourpkg.category.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ourpkg.category.Category1;
import ourpkg.category.dto.Category1CreateDTO;

@Mapper(componentModel = "spring")
public interface Category1CreateMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "category2List", ignore = true)
	@Mapping(target = "productList", ignore = true)
	Category1 toEntity(Category1CreateDTO dto);
}
