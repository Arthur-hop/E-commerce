package ourpkg.payment.controller;

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
import ourpkg.payment.dto.PaymentStatusCreateDTO;
import ourpkg.payment.dto.PaymentStatusResDTO;
import ourpkg.payment.dto.PaymentStatusUpdateDTO;
import ourpkg.payment.service.PaymentStatusService;

@RestController
@RequestMapping("/api/paymentStatus")
@RequiredArgsConstructor
public class PaymentStatusController {

	private final PaymentStatusService service;

	/** 查詢所有支付狀態（無條件） */
	@GetMapping("/all")
	public ResponseEntity<List<PaymentStatusResDTO>> getAll() {
		return ResponseEntity.ok(service.getAll());
	}

	/** 查詢單一（根據本表ID=支付狀態ID） */
	@GetMapping("/{id}")
	public ResponseEntity<PaymentStatusResDTO> getById(@PathVariable Integer id) {
		return ResponseEntity.ok(service.getById(id));
	}

	/** 新增支付狀態 */
	@PostMapping
	public ResponseEntity<PaymentStatusResDTO> create(@RequestBody @Valid PaymentStatusCreateDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
	}

	/** 更新支付狀態 */
	@PutMapping("/{id}")
	public ResponseEntity<PaymentStatusResDTO> update(@PathVariable Integer id, @RequestBody @Valid PaymentStatusUpdateDTO dto) {
		return ResponseEntity.ok(service.update(id, dto));
	}

	/**
	 * 刪除支付狀態（需先檢查該支付狀態 是否存在於 支付）
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Integer id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
