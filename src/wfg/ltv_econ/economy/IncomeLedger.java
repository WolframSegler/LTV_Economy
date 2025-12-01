package wfg.ltv_econ.economy;

public class IncomeLedger {
    public long monthlyExportIncome = 0;
    public long monthlyImportExpense = 0;

    public long lastMonthExportIncome = 0;
    public long lastMonthImportExpense = 0;

    public IncomeLedger() {}

    public void recordExport(int credits) {
        monthlyExportIncome += credits;
    }

    public void recordImport(int credits) {
        monthlyImportExpense += credits;
    }

    public void endMonth() {
        lastMonthExportIncome = monthlyExportIncome;
        lastMonthImportExpense = monthlyImportExpense;

        monthlyExportIncome = 0;
        monthlyImportExpense = 0;
    }
}