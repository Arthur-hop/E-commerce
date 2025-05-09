package ourpkg.category.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ourpkg.category.dto.Category2CreateDTO;
import ourpkg.category.dto.Category2ResDTO;
import ourpkg.category.dto.Category2UpdateDTO;
import ourpkg.category.service.Category2Service;

@RestController
@RequestMapping("/api/category2")
@RequiredArgsConstructor
public class Category2Controller {

	private final Category2Service service;

	/** 查詢所有二級分類（無條件） */
	@GetMapping("/all")
	public ResponseEntity<List<Category2ResDTO>> getAll() {
		return ResponseEntity.ok(service.getAll());
	}

	/** 查詢所有（有條件的 - 根據一級分類ID） */
	@GetMapping("/byC1")
	public ResponseEntity<List<Category2ResDTO>> getByCategory1(@RequestParam Integer category1Id) {
		return ResponseEntity.ok(service.getByCategory1(category1Id));
	}

	/** 查詢單一（根據本表ID=二級分類ID） */
	@GetMapping("/{id}")
	public ResponseEntity<Category2ResDTO> getById(@PathVariable Integer id) {
		return ResponseEntity.ok(service.getById(id));
	}

	/** 新增二級分類 */
	@PostMapping
	public ResponseEntity<Category2ResDTO> create(@RequestBody @Valid Category2CreateDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
	}

	/** 更新二級分類（不允許變更關聯的一級分類） */
	@PutMapping("/{id}")
	public ResponseEntity<Category2ResDTO> update(@PathVariable Integer id, @RequestBody @Valid Category2UpdateDTO dto) {
		return ResponseEntity.ok(service.update(id, dto));
	}

	/** 刪除二級分類（需先檢查該分類下是否有商品） */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Integer id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
