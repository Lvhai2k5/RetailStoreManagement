package ute.fit.service;

import ute.fit.dto.RevenueReportDTO;

public interface IRevenueService {
    RevenueReportDTO getFullReport(String range, String start, String end);
}
