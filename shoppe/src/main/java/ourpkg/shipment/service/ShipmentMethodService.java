package ourpkg.shipment.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ourpkg.shipment.ShipmentMethod;
import ourpkg.shipment.ShipmentMethodRepository;
import ourpkg.shipment.ShipmentRepository;
import ourpkg.shipment.dto.ShipmentMethodCreateDTO;
import ourpkg.shipment.dto.ShipmentMethodResDTO;
import ourpkg.shipment.dto.ShipmentMethodUpdateDTO;
import ourpkg.shipment.mapper.ShipmentMethodCreateMapper;
import ourpkg.shipment.mapper.ShipmentMethodResMapper;
import ourpkg.shipment.mapper.ShipmentMethodUpdateMapper;

@Service
@RequiredArgsConstructor
public class ShipmentMethodService {

	private final ShipmentMethodRepository repo;
	private final ShipmentRepository shipmentRepo; // 不需要管理的關聯
	private final ShipmentMethodCreateMapper createMapper;
	private final ShipmentMethodUpdateMapper updateMapper;
	private final ShipmentMethodResMapper resMapper;
	
	/** 查詢所有配送方式（無條件） */
	public List<ShipmentMethodResDTO> getAll() {
		return resMapper.toDtoList(repo.findAll());
	}

	/** 查詢單一（根據本表ID=配送方式ID） */
	public ShipmentMethodResDTO getById(Integer id) {
		return repo.findById(id).map(resMapper::toDto).orElseThrow(() -> new IllegalArgumentException("該配送方式 ID 不存在"));
	}

	/** 新增配送方式 */
	public ShipmentMethodResDTO create(ShipmentMethodCreateDTO dto) {
		ShipmentMethod entity = createMapper.toEntity(dto);
		return resMapper.toDto(repo.save(entity));
	}

	/** 更新配送方式 */
	public ShipmentMethodResDTO update(Integer id, ShipmentMethodUpdateDTO dto) {
		ShipmentMethod entity = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("該配送方式 ID 不存在"));

		entity = updateMapper.toEntity(entity, dto);
		return resMapper.toDto(repo.save(entity));
	}

	/** 刪除配送方式（需先檢查該配送方式 是否存在於 物流）*/
	public void delete(Integer id) {
		ShipmentMethod entity = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("該配送方式 ID 不存在"));

		if (shipmentRepo.existsByShipmentMethod_Id(id)) {
			throw new IllegalStateException("該配送方式被物流紀錄使用，請先移除物流紀錄");
		}

		repo.delete(entity);
	}
}
