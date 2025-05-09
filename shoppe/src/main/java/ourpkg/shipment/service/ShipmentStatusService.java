package ourpkg.shipment.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ourpkg.shipment.ShipmentRepository;
import ourpkg.shipment.ShipmentStatus;
import ourpkg.shipment.ShipmentStatusRepository;
import ourpkg.shipment.dto.ShipmentStatusCreateDTO;
import ourpkg.shipment.dto.ShipmentStatusResDTO;
import ourpkg.shipment.dto.ShipmentStatusUpdateDTO;
import ourpkg.shipment.mapper.ShipmentStatusCreateMapper;
import ourpkg.shipment.mapper.ShipmentStatusResMapper;
import ourpkg.shipment.mapper.ShipmentStatusUpdateMapper;

@Service
@RequiredArgsConstructor
public class ShipmentStatusService {

	private final ShipmentStatusRepository repo;
	private final ShipmentRepository shipmentRepo; // 不需要管理的關聯
	private final ShipmentStatusCreateMapper createMapper;
	private final ShipmentStatusUpdateMapper updateMapper;
	private final ShipmentStatusResMapper resMapper;
	
	/** 查詢所有配送狀態（無條件） */
	public List<ShipmentStatusResDTO> getAll() {
		return resMapper.toDtoList(repo.findAll());
	}

	/** 查詢單一（根據本表ID=配送狀態ID） */
	public ShipmentStatusResDTO getById(Integer id) {
		return repo.findById(id).map(resMapper::toDto).orElseThrow(() -> new IllegalArgumentException("該配送狀態 ID 不存在"));
	}

	/** 新增配送狀態 */
	public ShipmentStatusResDTO create(ShipmentStatusCreateDTO dto) {
		ShipmentStatus entity = createMapper.toEntity(dto);
		return resMapper.toDto(repo.save(entity));
	}

	/** 更新配送狀態 */
	public ShipmentStatusResDTO update(Integer id, ShipmentStatusUpdateDTO dto) {
		ShipmentStatus entity = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("該配送狀態 ID 不存在"));

		entity = updateMapper.toEntity(entity, dto);
		return resMapper.toDto(repo.save(entity));
	}

	/** 刪除配送狀態（需先檢查該配送狀態 是否存在於 物流）*/
	public void delete(Integer id) {
		ShipmentStatus entity = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("該配送狀態 ID 不存在"));

		if (shipmentRepo.existsByShipmentStatus_Id(id)) {
			throw new IllegalStateException("該配送狀態被物流紀錄使用，請先移除物流紀錄");
		}

		repo.delete(entity);
	}
}
