package ourpkg.payment.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ourpkg.payment.PaymentRepository;
import ourpkg.payment.PaymentStatus;
import ourpkg.payment.PaymentStatusRepository;
import ourpkg.payment.dto.PaymentStatusCreateDTO;
import ourpkg.payment.dto.PaymentStatusResDTO;
import ourpkg.payment.dto.PaymentStatusUpdateDTO;
import ourpkg.payment.mapper.PaymentStatusCreateMapper;
import ourpkg.payment.mapper.PaymentStatusResMapper;
import ourpkg.payment.mapper.PaymentStatusUpdateMapper;

@Service
@RequiredArgsConstructor
public class PaymentStatusService {

	private final PaymentStatusRepository repo;
	private final PaymentRepository paymentRepo; // 不需要管理的關聯
	private final PaymentStatusCreateMapper createMapper;
	private final PaymentStatusUpdateMapper updateMapper;
	private final PaymentStatusResMapper resMapper;
	
	/** 查詢所有支付狀態（無條件） */
	public List<PaymentStatusResDTO> getAll() {
		return resMapper.toDtoList(repo.findAll());
	}

	/** 查詢單一（根據本表ID=支付狀態ID） */
	public PaymentStatusResDTO getById(Integer id) {
		return repo.findById(id).map(resMapper::toDto).orElseThrow(() -> new IllegalArgumentException("該支付狀態 ID 不存在"));
	}

	/** 新增支付狀態 */
	public PaymentStatusResDTO create(PaymentStatusCreateDTO dto) {
		PaymentStatus entity = createMapper.toEntity(dto);
		return resMapper.toDto(repo.save(entity));
	}

	/** 更新支付狀態 */
	public PaymentStatusResDTO update(Integer id, PaymentStatusUpdateDTO dto) {
		PaymentStatus entity = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("該支付狀態 ID 不存在"));

		entity = updateMapper.toEntity(entity, dto);
		return resMapper.toDto(repo.save(entity));
	}

	/** 刪除支付狀態（需先檢查該支付狀態 是否存在於 支付）*/
	public void delete(Integer id) {
		PaymentStatus entity = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("該支付狀態 ID 不存在"));

		if (paymentRepo.existsByPaymentStatus_Id(id)) {
			throw new IllegalStateException("該支付狀態被支付紀錄使用，請先移除支付紀錄");
		}

		repo.delete(entity);
	}
}
