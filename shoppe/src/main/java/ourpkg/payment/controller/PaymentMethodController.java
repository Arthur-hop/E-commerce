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
import ourpkg.payment.dto.PaymentMethodCreateDTO;
import ourpkg.payment.dto.PaymentMethodResDTO;
import ourpkg.payment.dto.PaymentMethodUpdateDTO;
import ourpkg.payment.service.PaymentMethodService;

@RestController
@RequestMapping("/api/paymentMethod")
@RequiredArgsConstructor
public class PaymentMethodController {

	private final PaymentMethodService service;

	/** 查詢所有支付方式（無條件） */
	@GetMapping("/all")
	public ResponseEntity<List<PaymentMethodResDTO>> getAll() {
		return ResponseEntity.ok(service.getAll());
	}

	/** 查詢單一（根據本表ID=支付方式ID） */
	@GetMapping("/{id}")
	public ResponseEntity<PaymentMethodResDTO> getById(@PathVariable Integer id) {
		return ResponseEntity.ok(service.getById(id));
	}

	/** 新增支付方式 */
	@PostMapping
	public ResponseEntity<PaymentMethodResDTO> create(@RequestBody @Valid PaymentMethodCreateDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
	}

	/** 更新支付方式 */
	@PutMapping("/{id}")
	public ResponseEntity<PaymentMethodResDTO> update(@PathVariable Integer id, @RequestBody @Valid PaymentMethodUpdateDTO dto) {
		return ResponseEntity.ok(service.update(id, dto));
	}

	/**
	 * 刪除支付方式（需先檢查該支付方式 是否存在於 支付）
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Integer id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}





