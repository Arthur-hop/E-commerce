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
import ourpkg.shipment.dto.ShipmentStatusCreateDTO;
import ourpkg.shipment.dto.ShipmentStatusResDTO;
import ourpkg.shipment.dto.ShipmentStatusUpdateDTO;
import ourpkg.shipment.service.ShipmentStatusService;

@RestController
@RequestMapping("/api/shipmentStatus")
@RequiredArgsConstructor
public class ShipmentStatusController {

	private final ShipmentStatusService service;

	/** 查詢所有配送狀態（無條件） */
	@GetMapping("/all")
	public ResponseEntity<List<ShipmentStatusResDTO>> getAll() {
		return ResponseEntity.ok(service.getAll());
	}

	/** 查詢單一（根據本表ID=配送狀態ID） */
	@GetMapping("/{id}")
	public ResponseEntity<ShipmentStatusResDTO> getById(@PathVariable Integer id) {
		return ResponseEntity.ok(service.getById(id));
	}

	/** 新增配送狀態 */
	@PostMapping
	public ResponseEntity<ShipmentStatusResDTO> create(@RequestBody @Valid ShipmentStatusCreateDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
	}

	/** 更新配送狀態 */
	@PutMapping("/{id}")
	public ResponseEntity<ShipmentStatusResDTO> update(@PathVariable Integer id, @RequestBody @Valid ShipmentStatusUpdateDTO dto) {
		return ResponseEntity.ok(service.update(id, dto));
	}

	/**
	 * 刪除配送狀態（需先檢查該配送狀態 是否存在於 物流）
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Integer id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
