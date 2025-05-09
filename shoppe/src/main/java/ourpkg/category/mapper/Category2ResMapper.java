package ourpkg.category.mapper;

import java.util.Collections;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import ourpkg.category.Category1;
import ourpkg.category.Category2;
import ourpkg.category.dto.Category2ResDTO;

@Mapper(componentModel = "spring")
public interface Category2ResMapper {

	@Mapping(target = "category1Ids", source = "category1List", qualifiedByName = "mapCategory1Ids")
	Category2ResDTO toDto(Category2 entity);

	List<Category2ResDTO> toDtoList(List<Category2> entityList);
	
	@Named("mapCategory1Ids")
    default List<Integer> mapCategory1Ids(List<Category1> category1List) {
        if (category1List == null) return Collections.emptyList();
        return category1List.stream().map(Category1::getId).toList();
    }

}
