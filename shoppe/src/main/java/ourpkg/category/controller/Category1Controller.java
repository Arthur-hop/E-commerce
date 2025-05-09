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
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ourpkg.category.dto.Category1CreateDTO;
import ourpkg.category.dto.Category1ResDTO;
import ourpkg.category.dto.Category1UpdateDTO;
import ourpkg.category.service.Category1Service;

@RestController
@RequestMapping("/api/category1")
@RequiredArgsConstructor
public class Category1Controller {

	private final Category1Service service;
	
	/** 查詢所有一級分類（無條件） */
	@GetMapping("/all")
	public ResponseEntity<List<Category1ResDTO>> getAll() {
		return ResponseEntity.ok(service.getAll());
	}

	/** 查詢單一（根據本表ID=一級分類ID） */
	@GetMapping("/{id}")
	public ResponseEntity<Category1ResDTO> getById(@PathVariable Integer id) {
		return ResponseEntity.ok(service.getById(id));
	}

	/** 新增一級分類 */
	@PostMapping
	public ResponseEntity<Category1ResDTO> create(@RequestBody @Valid Category1CreateDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
	}

	/** 更新一級分類 */
	@PutMapping("/{id}")
	public ResponseEntity<Category1ResDTO> update(@PathVariable Integer id, @RequestBody @Valid Category1UpdateDTO dto) {
		return ResponseEntity.ok(service.update(id, dto));
	}

	/** 刪除一級分類（需先檢查該一級分類下是否有二級分類） */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Integer id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
