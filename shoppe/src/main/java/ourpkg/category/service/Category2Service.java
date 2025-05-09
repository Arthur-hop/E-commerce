package ourpkg.category.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ourpkg.category.Category1;
import ourpkg.category.Category2;
import ourpkg.category.dto.Category2CreateDTO;
import ourpkg.category.dto.Category2ResDTO;
import ourpkg.category.dto.Category2UpdateDTO;
import ourpkg.category.mapper.Category2CreateMapper;
import ourpkg.category.mapper.Category2ResMapper;
import ourpkg.category.mapper.Category2UpdateMapper;
import ourpkg.category.repo.Category1Repository;
import ourpkg.category.repo.Category2Repository;
import ourpkg.product.ProductRepository;

@Service
@RequiredArgsConstructor
public class Category2Service {

	private final Category2Repository repo;
	private final Category1Repository category1Repo; // 需要管理的關聯
	private final ProductRepository productRepo; // 不需要管理的關聯
	private final Category2CreateMapper createMapper;
	private final Category2UpdateMapper updateMapper;
	private final Category2ResMapper resMapper;

	/** 查詢所有二級分類（無條件） */
	public List<Category2ResDTO> getAll() {
		return resMapper.toDtoList(repo.findAll());
	}

	/** 查詢所有（有條件的 - 根據一級分類ID） */
	public List<Category2ResDTO> getByCategory1(Integer category1Id) {
		if (!category1Repo.existsById(category1Id)) {
			throw new IllegalArgumentException("該一級分類 ID 不存在");
		}
		return resMapper.toDtoList(repo.findByCategory1List_Id(category1Id));
	}

	/** 查詢單一（根據本表ID=二級分類ID） */
	public Category2ResDTO getById(Integer id) {
		return repo.findById(id).map(resMapper::toDto).orElseThrow(() -> new IllegalArgumentException("該二級分類 ID 不存在"));
	}

	/** 新增二級分類 */
	public Category2ResDTO create(Category2CreateDTO dto) {
		List<Category1> category1List = category1Repo.findAllById(dto.getCategory1Ids());
		if (category1List.size() != dto.getCategory1Ids().size()) {
			throw new IllegalArgumentException("部分一級分類 ID 不存在");
		}

		Category2 entity = createMapper.toEntity(dto);
		entity.setCategory1List(category1List);
		return resMapper.toDto(repo.save(entity));
	}

	/** 更新二級分類（不允許變更關聯的一級分類） */
	public Category2ResDTO update(Integer id, Category2UpdateDTO dto) {
		Category2 entity = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("該二級分類 ID 不存在"));

		entity = updateMapper.toEntity(entity, dto);
		return resMapper.toDto(repo.save(entity));
	}

	/** 刪除二級分類（需先檢查該分類下是否有商品） */
	public void delete(Integer id) {
		Category2 entity = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("該二級分類 ID 不存在"));

		if (productRepo.existsByCategory2_Id(id)) {
			throw new IllegalStateException("該二級分類下仍有商品，請先移除商品");
		}

		repo.delete(entity);
	}
}
