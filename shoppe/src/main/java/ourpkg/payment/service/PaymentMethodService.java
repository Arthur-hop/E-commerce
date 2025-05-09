package ourpkg.payment.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ourpkg.payment.PaymentMethod;
import ourpkg.payment.PaymentMethodRepository;
import ourpkg.payment.PaymentRepository;
import ourpkg.payment.dto.PaymentMethodCreateDTO;
import ourpkg.payment.dto.PaymentMethodResDTO;
import ourpkg.payment.dto.PaymentMethodUpdateDTO;
import ourpkg.payment.mapper.PaymentMethodCreateMapper;
import ourpkg.payment.mapper.PaymentMethodResMapper;
import ourpkg.payment.mapper.PaymentMethodUpdateMapper;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

	private final PaymentMethodRepository repo;
	private final PaymentRepository paymentRepo; // 不需要管理的關聯
	private final PaymentMethodCreateMapper createMapper;
	private final PaymentMethodUpdateMapper updateMapper;
	private final PaymentMethodResMapper resMapper;
	
	/** 查詢所有支付方式（無條件） */
	public List<PaymentMethodResDTO> getAll() {
		return resMapper.toDtoList(repo.findAll());
	}

	/** 查詢單一（根據本表ID=支付方式ID） */
	public PaymentMethodResDTO getById(Integer id) {
		return repo.findById(id).map(resMapper::toDto).orElseThrow(() -> new IllegalArgumentException("該支付方式 ID 不存在"));
	}

	/** 新增支付方式 */
	public PaymentMethodResDTO create(PaymentMethodCreateDTO dto) {
		PaymentMethod entity = createMapper.toEntity(dto);
		return resMapper.toDto(repo.save(entity));
	}

	/** 更新支付方式 */
	public PaymentMethodResDTO update(Integer id, PaymentMethodUpdateDTO dto) {
		PaymentMethod entity = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("該支付方式 ID 不存在"));

		entity = updateMapper.toEntity(entity, dto);
		return resMapper.toDto(repo.save(entity));
	}

	/** 刪除支付方式（需先檢查該支付方式 是否存在於 支付）*/
	public void delete(Integer id) {
		PaymentMethod entity = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("該支付方式 ID 不存在"));

		if (paymentRepo.existsByPaymentMethod_Id(id)) {
			throw new IllegalStateException("該支付方式被支付紀錄使用，請先移除支付紀錄");
		}

		repo.delete(entity);
	}
}
