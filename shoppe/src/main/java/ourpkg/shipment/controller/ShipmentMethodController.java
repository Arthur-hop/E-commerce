package ourpkg.shipment.controller;

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
import ourpkg.shipment.dto.ShipmentMethodCreateDTO;
import ourpkg.shipment.dto.ShipmentMethodResDTO;
import ourpkg.shipment.dto.ShipmentMethodUpdateDTO;
import ourpkg.shipment.service.ShipmentMethodService;

@RestController
@RequestMapping("/api/shipmentMethod")
@RequiredArgsConstructor
public class ShipmentMethodController {

	private final ShipmentMethodService service;

	/** 查詢所有配送方式（無條件） */
	@GetMapping("/all")
	public ResponseEntity<List<ShipmentMethodResDTO>> getAll() {
		return ResponseEntity.ok(service.getAll());
	}

	/** 查詢單一（根據本表ID=配送方式ID） */
	@GetMapping("/{id}")
	public ResponseEntity<ShipmentMethodResDTO> getById(@PathVariable Integer id) {
		return ResponseEntity.ok(service.getById(id));
	}

	/** 新增配送方式 */
	@PostMapping
	public ResponseEntity<ShipmentMethodResDTO> create(@RequestBody @Valid ShipmentMethodCreateDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
	}

	/** 更新配送方式 */
	@PutMapping("/{id}")
	public ResponseEntity<ShipmentMethodResDTO> update(@PathVariable Integer id, @RequestBody @Valid ShipmentMethodUpdateDTO dto) {
		return ResponseEntity.ok(service.update(id, dto));
	}

	/**
	 * 刪除配送方式（需先檢查該配送方式 是否存在於 物流）
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Integer id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}





