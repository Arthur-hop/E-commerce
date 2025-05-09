package ourpkg.category.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ourpkg.category.Category1;
import ourpkg.category.dto.Category1CreateDTO;
import ourpkg.category.dto.Category1ResDTO;
import ourpkg.category.dto.Category1UpdateDTO;
import ourpkg.category.mapper.Category1CreateMapper;
import ourpkg.category.mapper.Category1ResMapper;
import ourpkg.category.mapper.Category1UpdateMapper;
import ourpkg.category.repo.Category1Repository;
import ourpkg.category.repo.Category2Repository;

@Service
@RequiredArgsConstructor
public class Category1Service {

	private final Category1Repository repo;
	private final Category2Repository category2Repo; // 不需要管理的關聯
	private final Category1CreateMapper createMapper;
	private final Category1UpdateMapper updateMapper;
	private final Category1ResMapper resMapper;
	
	/** 查詢所有一級分類（無條件） */
	public List<Category1ResDTO> getAll() {
		return resMapper.toDtoList(repo.findAll());
	}

	/** 查詢單一（根據本表ID=一級分類ID） */
	public Category1ResDTO getById(Integer id) {
		return repo.findById(id).map(resMapper::toDto).orElseThrow(() -> new IllegalArgumentException("該一級分類 ID 不存在"));
	}

	/** 新增一級分類 */
	public Category1ResDTO create(Category1CreateDTO dto) {
		Category1 entity = createMapper.toEntity(dto);
		return resMapper.toDto(repo.save(entity));
	}

	/** 更新一級分類*/
	public Category1ResDTO update(Integer id, Category1UpdateDTO dto) {
		Category1 entity = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("該一級分類 ID 不存在"));

		entity = updateMapper.toEntity(entity, dto);
		return resMapper.toDto(repo.save(entity));
	}

	/** 刪除一級分類（需先檢查該一級分類下是否有二級分類） */
	public void delete(Integer id) {
		Category1 entity = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("該一級分類 ID 不存在"));

		if (category2Repo.existsByCategory1List_Id(id)) {
			throw new IllegalStateException("該一級分類下仍有二級分類，請先移除二級分類");
		}

		repo.delete(entity);
	}
}
