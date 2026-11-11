package id.ac.sekolah.manajemenkelas.dashboard.service;

import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.dashboard.model.DashboardSummary;
import id.ac.sekolah.manajemenkelas.report.dao.ReportDAO;

public class DashboardService {

    private final ReportDAO reportDAO;

    public DashboardService(ReportDAO reportDAO) {
        if (reportDAO == null) {
            throw new IllegalArgumentException("ReportDAO tidak boleh null.");
        }
        this.reportDAO = reportDAO;
    }

    public DashboardSummary getDashboardSummary() {
        Long teacherIdScope = SessionContext.getInstance().resolveTeacherScopeOrNullForAdmin("dashboard");
        return reportDAO.getDashboardSummary(teacherIdScope);
    }
}
