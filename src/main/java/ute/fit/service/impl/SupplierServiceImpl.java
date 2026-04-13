package ute.fit.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ute.fit.dto.SupplierFormDTO;
import ute.fit.dto.SupplierViewDTO;
import ute.fit.entity.SuppliersEntity;
import ute.fit.model.SupplierStatus;
import ute.fit.repository.SupplierRepository;
import ute.fit.service.ISupplierService;
import ute.fit.util.SupplierMapper;

import java.util.List;

@Service
public class SupplierServiceImpl implements ISupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierServiceImpl(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierViewDTO> getSuppliers(String keyword) {
        return supplierRepository.search(keyword).stream()
                .map(SupplierMapper::toViewDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierFormDTO getSupplierForm(Integer supplierId) {
        SuppliersEntity entity = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà cung cấp."));
        return SupplierMapper.toFormDTO(entity);
    }

    @Override
    @Transactional
    public void saveSupplier(SupplierFormDTO form) {
        validate(form);

        if (form.getSupplierID() == null) {
            supplierRepository.save(SupplierMapper.toNewEntity(form));
            return;
        }

        SuppliersEntity existing = supplierRepository.findById(form.getSupplierID())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà cung cấp để cập nhật."));

        if (form.getStatus() == null) {
            form.setStatus(existing.getStatus());
        }
        SupplierMapper.copyFormToEntity(form, existing);
        supplierRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteSupplier(Integer supplierId) {
        if (!supplierRepository.existsById(supplierId)) {
            throw new IllegalArgumentException("Nhà cung cấp không tồn tại.");
        }
        supplierRepository.deleteById(supplierId);
    }

    @Override
    @Transactional
    public void toggleSupplierStatus(Integer supplierId) {
        SuppliersEntity entity = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Nhà cung cấp không tồn tại."));

        SupplierStatus next = entity.getStatus() == SupplierStatus.COLLAB
                ? SupplierStatus.STOP
                : SupplierStatus.COLLAB;

        entity.setStatus(next);
        supplierRepository.save(entity);
    }

    private void validate(SupplierFormDTO form) {
        if (form.getName() == null || form.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên nhà cung cấp không được để trống.");
        }
        if (form.getPhone() == null || form.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Số điện thoại không được để trống.");
        }
        if (form.getAddress() == null || form.getAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Địa chỉ không được để trống.");
        }

        form.setName(form.getName().trim());
        form.setPhone(form.getPhone().trim());
        form.setAddress(form.getAddress().trim());
        if (form.getStatus() == null) {
            form.setStatus(SupplierStatus.COLLAB);
        }
    }
}
