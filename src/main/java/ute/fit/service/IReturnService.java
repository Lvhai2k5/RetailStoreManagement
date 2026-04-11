package ute.fit.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ute.fit.dto.returns.ReturnCreateDTO;
import ute.fit.dto.returns.ReturnDTO;
import ute.fit.dto.returns.ReturnOrderFormDTO;
import ute.fit.dto.returns.ReturnOrderSummaryDTO;

public interface IReturnService {
    Page<ReturnDTO> getAllReturns(Pageable pageable);
    ReturnDTO getReturnById(Integer returnId);
    ReturnDTO createReturn(ReturnCreateDTO request);
    List<ReturnOrderSummaryDTO> getPaidOrders();
    ReturnOrderFormDTO getReturnOrderForm(Integer orderId);
}
