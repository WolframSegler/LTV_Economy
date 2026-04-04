package wfg.ltv_econ.util;

import java.io.Serializable;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.combat.MutableStat.StatModType;

import wfg.native_ui.util.ArrayMap;

import com.fs.starfarer.api.combat.StatBonus;

public class ArrayMutableStat implements Serializable {
	private static final String BASE_MOD_ID = "base";

	private final ArrayMap<String, StatMod> baseMods;
    private final ArrayMap<String, StatMod> flatMods;
	private final ArrayMap<String, StatMod> percentMods;
	private final ArrayMap<String, StatMod> multMods;

	private float modified;
	
	private transient boolean needsRecompute = false;
	
	private transient float base;
	private transient float flatMod;
	private transient float percentMod;
	private transient float mult = 1f;

	public ArrayMutableStat(float base) {
		this(base, 4);
	}

	public ArrayMutableStat(float base, int modMapsInitSize) {
		this(base, modMapsInitSize, modMapsInitSize, modMapsInitSize, modMapsInitSize);
	}
	
	public ArrayMutableStat(float base, int baseModsInitSize, int flatModsInitSize, int percentModsInitSize, int multModsInitSize) {
		this.base = base;
		modified = base;

		baseMods = new ArrayMap<>(baseModsInitSize);
		flatMods = new ArrayMap<>(flatModsInitSize);
		percentMods = new ArrayMap<>(percentModsInitSize);
		multMods = new ArrayMap<>(multModsInitSize);

		modifyBase(BASE_MOD_ID, base);
	}
	
	protected final Object readResolve() {
		mult = 1f;
		needsRecompute = true;

		return this;
	}
	
	public final ArrayMutableStat createCopy() {
		final ArrayMutableStat copy = new ArrayMutableStat(
			getBaseValue(), baseMods.size(), flatMods.size(), percentMods.size(), multMods.size()
		);
		copy.applyMods(this);
		return copy;
	}

    public final void applyMods(ArrayMutableStat other) {
		baseMods.putAll(other.baseMods);
		flatMods.putAll(other.flatMods);
		percentMods.putAll(other.percentMods);
		multMods.putAll(other.multMods);
		needsRecompute = true;
	}
	
	public final void applyMods(MutableStat other) {
		flatMods.putAll(other.getFlatMods());
		percentMods.putAll(other.getPercentMods());
		multMods.putAll(other.getMultMods());
		needsRecompute = true;
	}
	
	public final void applyMods(StatBonus other) {
		flatMods.putAll(other.getFlatBonuses());
		percentMods.putAll(other.getPercentBonuses());
		multMods.putAll(other.getMultBonuses());
		needsRecompute = true;
	}
	
	public final boolean isUnmodified() {
		return flatMods.isEmpty() &&
			percentMods.isEmpty() &&
			multMods.isEmpty();
	}

	public ArrayMap<String, StatMod> getBaseMods() {
		return baseMods;
	}

	public ArrayMap<String, StatMod> getFlatMods() {
		return flatMods;
	}

	public ArrayMap<String, StatMod> getPercentMods() {
		return percentMods;
	}

	public ArrayMap<String, StatMod> getMultMods() {
		return multMods;
	}

	public StatMod getBaseStatMod(String source) {
		return baseMods.get(source);
	}

	public StatMod getFlatStatMod(String source) {
		return flatMods.get(source);
	}
	
	public StatMod getPercentStatMod(String source) {
		return percentMods.get(source);
	}
	
	public StatMod getMultStatMod(String source) {
		return multMods.get(source);
	}
	
	public final void modifyBase(String source, float value) {
		modifyBase(source, value, null);
	}
	
	public final void modifyBase(String source, float value, String desc) {
        if (value == 0f) { baseMods.remove(source); return; }

        final StatMod mod = baseMods.get(source);
        if (mod != null) {
            needsRecompute = mod.value != value;
            mod.value = value;
            mod.desc = desc;
        } else {
            needsRecompute = true;
            baseMods.put(source, new StatMod(source, StatModType.FLAT, value, desc));
        }
	}

	public final void modifyFlat(String source, float value) {
		modifyFlat(source, value, null);
	}
	
	public final void modifyFlat(String source, float value, String desc) {
        if (value == 0f) { flatMods.remove(source); return; }

        final StatMod mod = flatMods.get(source);
        if (mod != null) {
            needsRecompute = mod.value != value;
            mod.value = value;
            mod.desc = desc;
        } else {
            needsRecompute = true;
            flatMods.put(source, new StatMod(source, StatModType.FLAT, value, desc));
        }
	}
	
	public final void modifyPercent(String source, float value) {
		modifyPercent(source, value, null);
	}
	
	public final void modifyPercent(String source, float value, String desc) {
        if (value == 0f) { percentMods.remove(source); return; }

        final StatMod mod = percentMods.get(source);
        if (mod != null) {
            needsRecompute = mod.value != value;
            mod.value = value;
            mod.desc = desc;
        } else {
            needsRecompute = true;
            percentMods.put(source, new StatMod(source, StatModType.PERCENT, value, desc));
        }
	}
	
	public final void modifyMult(String source, float value) {
		modifyMult(source, value, null);
	}
	
	public final void modifyMult(String source, float value, String desc) {
        if (value == 1f) { multMods.remove(source); return; }

        final StatMod mod = multMods.get(source);
        if (mod != null) {
            needsRecompute = mod.value != value;
            mod.value = value;
            mod.desc = desc;
        } else {
            needsRecompute = true;
            multMods.put(source, new StatMod(source, StatModType.MULT, value, desc));
        }
	}
	
	public final void modifyBaseAlways(String source, float value, String desc) {
		baseMods.put(source, new StatMod(source, StatModType.FLAT, value, desc));
		needsRecompute = true;
	}

	public final void modifyFlatAlways(String source, float value, String desc) {
		flatMods.put(source, new StatMod(source, StatModType.FLAT, value, desc));
		needsRecompute = true;
	}

    public final void modifyPercentAlways(String source, float value, String desc) {
		percentMods.put(source, new StatMod(source, StatModType.PERCENT, value, desc));
		needsRecompute = true;
	}

    public final void modifyMultAlways(String source, float value, String desc) {
		multMods.put(source, new StatMod(source, StatModType.MULT, value, desc));
		needsRecompute = true;
	}
	
	public final void unmodifyBase() {
		baseMods.clear();
		needsRecompute = true;
	}

	public final void unmodify() {
		flatMods.clear();
		percentMods.clear();
		multMods.clear();
		needsRecompute = true;
	}
	
	public final void unmodify(String source) {
        StatMod mod = flatMods.remove(source);
        if (mod != null && mod.value != 0f) needsRecompute = true;

        mod = percentMods.remove(source);
        if (mod != null && mod.value != 0f) needsRecompute = true;

        mod = multMods.remove(source);
        if (mod != null && mod.value != 1f) needsRecompute = true;
	}

	public final void unmodifyBase(String source) {
		final StatMod mod = baseMods.remove(source);
		if (mod != null && mod.value != 0) needsRecompute = true;
	}

	public final void unmodifyFlat(String source) {
		final StatMod mod = flatMods.remove(source);
		if (mod != null && mod.value != 0) needsRecompute = true;
	}
	
	public final void unmodifyPercent(String source) {
		final StatMod mod = percentMods.remove(source);
		if (mod != null && mod.value != 0) needsRecompute = true;
	}
	
	public final void unmodifyMult(String source) {
		final StatMod mod = multMods.remove(source);
		if (mod != null && mod.value != 1) needsRecompute = true;
	}	
	
	private final void recompute() {
		base = 0f;
		flatMod = 0f;
		percentMod = 0f;
		mult = 1f;

		for (StatMod mod : baseMods.values()) base += mod.value;

        for (StatMod mod : percentMods.values()) percentMod += mod.value;
        
        for (StatMod mod : flatMods.values()) flatMod += mod.value;
        
        for (StatMod mod : multMods.values()) mult *= mod.value;
        
		modified = (base + flatMod + (base * percentMod / 100f)) * mult;
		needsRecompute = false;
	}

	public final float getFlatMod() {
		if (needsRecompute) recompute();
		return flatMod;
	}

	public final float getPercentMod() {
		if (needsRecompute) recompute();
		return percentMod;
	}

	public final float getMult() {
		if (needsRecompute) recompute();
		return mult;
	}

	public final float computeMultMod() {
		float mult = 1f;
		for (StatMod mod : multMods.values()) mult *= mod.value;
		return mult;
	}
	
	public final float getModifiedValue() {
		if (needsRecompute) recompute();
		return modified;
	}
	
	public final int getModifiedInt() {
		if (needsRecompute) recompute();
		return (int) Math.round(modified);
	}
	
	public final float getBaseValue() {
		if (needsRecompute) recompute();
		return base;
	}
	
    public final void markDirty() {
        needsRecompute = true;
    }

	public final boolean isPositive() {
		return getModifiedValue() > getBaseValue();
	}
	
	public final boolean isNegative() {
		return getModifiedValue() < getBaseValue();
	}
}