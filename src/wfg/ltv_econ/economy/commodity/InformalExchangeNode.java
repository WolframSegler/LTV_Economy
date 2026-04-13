package wfg.ltv_econ.economy.commodity;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;

import wfg.ltv_econ.config.EconomyConfig;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.util.Arithmetic;

public class InformalExchangeNode {

    public final String comID;
    public transient CommoditySpecAPI spec;

    public float prod = 0f;
    public float imports = 0f;
    public float exports = 0f;
    public float priceMultImporting = 1f;
    public float priceMultExporting = 1f;
    public float tariffEnforcementImporting = 1f;
    public float tariffEnforcementExporting = 1f;

    public InformalExchangeNode(final String comID) {
        this.comID = comID;
        readResolve();
    }

    public Object readResolve() {
        spec = Global.getSettings().getCommoditySpec(comID);

        return this;
    }

    private static final float baseActivity = 0.035f;
    private static final float shortageResponse = 0.65f;
    private static final double shortageExp = 0.7;
    private static final float glutResistance = 0.25f;
    private static final float importAppetite = 0.55f;
    private static final float importSaturationPoint = 180_000f;
    private static final float arbitrageDrive = 0.4f;
    private static final float marketReach = 0.25f;

    public final void updateBeforeTrade() {
        final EconomyEngine engine = EconomyEngine.instance();
        final float informalFraction = 0.03f / EconomyConfig.TRADE_INTERVAL; // TODO turn into a config entry

        final float totalDemand;
        final float totalExcess;
        { // calculate demand & excess
            float demand = 0f;
            float excess = 0f;
            for (CommodityCell cell : engine.getComDomain(comID).getAllCells()) {
                demand += cell.computeImportAmount();
                excess += cell.computeExportAmount();
            }
            totalDemand = demand * informalFraction;
            totalExcess = excess * informalFraction;
        }

        final float imbalance = totalDemand - totalExcess;
        final float baselineActivity = baseActivity * (float) Math.sqrt(totalDemand + totalExcess + 1);

        prod = imbalance > 0f ?
            baselineActivity + (float) Math.pow(shortageResponse * imbalance, shortageExp) :
            baselineActivity * (1f + 1f / (1f + glutResistance * -imbalance));

        imports = importAppetite * totalExcess / (1f + totalExcess / importSaturationPoint)
            + Math.min(arbitrageDrive * Math.max(imbalance, 0f), totalExcess);

        exports = Math.min(prod + imports, totalDemand * marketReach);

        final float pressure = totalExcess / (1f + totalDemand);
        final float scarcity = totalDemand / (1f + totalExcess);

        priceMultImporting = 1f - Arithmetic.clamp(0.05f + 0.35f * pressure, 0.05f, 0.40f);
        priceMultExporting = 1f + Arithmetic.clamp(0.10f + 0.40f * scarcity, 0.10f, 0.60f);
        tariffEnforcementImporting = 0.15f + 0.30f * scarcity;
        tariffEnforcementExporting = 0.35f + 0.50f * scarcity;
    }
}