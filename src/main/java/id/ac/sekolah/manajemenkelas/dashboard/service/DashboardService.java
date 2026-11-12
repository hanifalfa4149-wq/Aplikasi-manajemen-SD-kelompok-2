package id.ac.sekolah.manajemenkelas.dashboard.service;

import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.dashboard.model.DashboardSummary;
import id.ac.sekolah.manajemenkelas.report.dao.ReportDAO;

public class DashboardService {

    private final ReportDAO daoLaporan;

    public DashboardService(ReportDAO daoLaporan) {
        if (daoLaporan == null) {
            throw new IllegalArgumentException("ReportDAO tidak boleh null.");
        }
        this.daoLaporan = daoLaporan;
    }

    public DashboardSummary getDashboardSummary() {
        Long idCakupanGuru = SessionContext.getInstance().resolveTeacherScopeOrNullForAdmin("dashboard");
        return daoLaporan.getDashboardSummary(idCakupanGuru);
    }
}
