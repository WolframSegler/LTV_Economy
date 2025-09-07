package wfg.ltv_econ.industry;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.combat.MutableStat.StatModType;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseInstallableItemEffect;
import com.fs.starfarer.api.util.Misc;

public abstract class LtvBoostIndustryInstallableItemEffect extends BaseInstallableItemEffect {

	protected float supplyIncrease = 0;
	protected float demandIncrease = 0;
	protected StatModType operationType = StatModType.FLAT;

	public LtvBoostIndustryInstallableItemEffect(String id, float supplyIncrease, float demandIncrease, StatModType operationType) {
		super(id);
		this.supplyIncrease = supplyIncrease;
		this.demandIncrease = demandIncrease;
		this.operationType = operationType;
	}

	public void apply(Industry industry) {
		if (supplyIncrease != 0) {
			switch (operationType) {
				case MULT:
					industry.getSupplyBonus().modifyMult(spec.getId(), supplyIncrease, Misc.ucFirst(spec.getName().toLowerCase()));
					break;
				case PERCENT:
					industry.getSupplyBonus().modifyPercent(spec.getId(), supplyIncrease, Misc.ucFirst(spec.getName().toLowerCase()));
					break;
				case FLAT:
					industry.getSupplyBonus().modifyFlat(spec.getId(), supplyIncrease, Misc.ucFirst(spec.getName().toLowerCase()));
					break;
				default:
			}
		}
		if (demandIncrease != 0) {
			switch (operationType) {
				case MULT:
					industry.getSupplyBonus().modifyMult(spec.getId(), -demandIncrease, Misc.ucFirst(spec.getName().toLowerCase()));
					break;
				case PERCENT:
					industry.getSupplyBonus().modifyPercent(spec.getId(), -demandIncrease, Misc.ucFirst(spec.getName().toLowerCase()));
					break;
				case FLAT:
					industry.getSupplyBonus().modifyFlat(spec.getId(), -demandIncrease, Misc.ucFirst(spec.getName().toLowerCase()));
					break;
				default:
			}
		}
	}
	public void unapply(Industry industry) {
		if (supplyIncrease != 0) {
			industry.getSupplyBonus().modifyFlat(spec.getId(), 0,
					Misc.ucFirst(spec.getName().toLowerCase()));
		}
		if (demandIncrease != 0) {
			industry.getDemandReduction().modifyFlat(spec.getId(), 0,
					Misc.ucFirst(spec.getName().toLowerCase()));
		}
	}

	public float getSupplyIncrease() {
		return supplyIncrease;
	}

	public void setSupplyIncrease(int supplyIncrease) {
		this.supplyIncrease = supplyIncrease;
	}

	public float getDemandIncrease() {
		return demandIncrease;
	}

	public void setDemandIncrease(int demandIncrease) {
		this.demandIncrease = demandIncrease;
	}
}






