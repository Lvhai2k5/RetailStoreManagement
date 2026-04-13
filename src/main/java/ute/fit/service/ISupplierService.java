package ute.fit.service;

import ute.fit.dto.SupplierFormDTO;
import ute.fit.dto.SupplierViewDTO;

import java.util.List;

public interface ISupplierService {

    List<SupplierViewDTO> getSuppliers(String keyword);

    SupplierFormDTO getSupplierForm(Integer supplierId);

    void saveSupplier(SupplierFormDTO form);

    void deleteSupplier(Integer supplierId);

    void toggleSupplierStatus(Integer supplierId);
}
