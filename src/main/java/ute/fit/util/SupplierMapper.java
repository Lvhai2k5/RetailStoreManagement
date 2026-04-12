package ute.fit.util;

import ute.fit.dto.SupplierFormDTO;
import ute.fit.dto.SupplierViewDTO;
import ute.fit.entity.SuppliersEntity;
import ute.fit.model.SupplierStatus;

public final class SupplierMapper {

    private SupplierMapper() {
    }

    public static SupplierViewDTO toViewDTO(SuppliersEntity entity) {
        SupplierViewDTO dto = new SupplierViewDTO();
        dto.setSupplierID(entity.getSupplierID());
        dto.setName(entity.getName());
        dto.setPhone(entity.getPhone());
        dto.setAddress(entity.getAddress());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    public static SupplierFormDTO toFormDTO(SuppliersEntity entity) {
        SupplierFormDTO dto = new SupplierFormDTO();
        dto.setSupplierID(entity.getSupplierID());
        dto.setName(entity.getName());
        dto.setPhone(entity.getPhone());
        dto.setAddress(entity.getAddress());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    public static SuppliersEntity toNewEntity(SupplierFormDTO form) {
        SuppliersEntity entity = new SuppliersEntity();
        copyFormToEntity(form, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(SupplierStatus.COLLAB);
        }
        return entity;
    }

    public static void copyFormToEntity(SupplierFormDTO form, SuppliersEntity entity) {
        entity.setName(form.getName());
        entity.setPhone(form.getPhone());
        entity.setAddress(form.getAddress());
        entity.setStatus(form.getStatus() == null ? SupplierStatus.COLLAB : form.getStatus());
    }
}
