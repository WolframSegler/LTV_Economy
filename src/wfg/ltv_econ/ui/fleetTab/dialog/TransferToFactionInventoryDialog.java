package wfg.ltv_econ.ui.fleetTab.dialog;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.UIConstants.*;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.plugins.LevelupPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.loading.specs.HullVariantSpec;

import rolflectionlib.util.RolfLectionUtil;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.fleet.ShipTypeData;
import wfg.native_ui.ui.dialog.DialogPanel;
import wfg.native_ui.util.Globals;

public class TransferToFactionInventoryDialog extends DialogPanel {

    private final FleetMemberAPI member;
    private final UIPanelAPI fleetList;
    
    public TransferToFactionInventoryDialog(FleetMemberAPI member, UIPanelAPI fleetList) {
        super(450, 120, null, null, str("uiConfirm"), str("uiCancel"));

        this.member = member;
        this.fleetList = fleetList;

        backgroundDimAmount = 0.5f;
        holo.borderAlpha = 0.66f;
        setConfirmShortcut();

        buildUI();
    }

    @Override
    public void buildUI() {
        final int crewNeeded = ShipTypeData.getCrewPerShip(member.getHullSpec());
        final String highlightStr1 = Integer.toString(Math.round(getBonusXpFraction(member.getVariant()) * 100f)) + "%";
        final String highlightStr2 = str("uiTxtBonusExperience");
        final String highlightStr3 = Integer.toString(crewNeeded);
        final LabelAPI txtLbl = Globals.settings.createLabel(
            strf("uiDialogTransferToFactionInventoryTxt", highlightStr1, highlightStr2, highlightStr3), Fonts.INSIGNIA_LARGE
        );
        add(txtLbl);
        txtLbl.setColor(text_color);
        txtLbl.setHighlightColor(Misc.getStoryBrightColor());
        txtLbl.setHighlight(highlightStr1, highlightStr2, highlightStr3);
        txtLbl.getPosition().setSize(pos.getWidth(), pos.getHeight() - BUTTON_H).inTL(0f, 0f);
        txtLbl.setAlignment(Alignment.TL);

        final boolean hasEnoughCrew = Global.getSector().getPlayerFleet().getCargo().getCrew() >= crewNeeded;
        getButton(0).setEnabled(hasEnoughCrew);
    }

    @Override
    public void dismiss(int option) {
        super.dismiss(option);

        if (option != 0) return;

        EconomyEngine.instance().getFactionShipInventory(Factions.PLAYER).addShip(member.getHullId(), 1);

        final CargoAPI playerCargo = Global.getSector().getPlayerFleet().getCargo();
        final HullVariantSpec variant = (HullVariantSpec) member.getVariant();

        if (!Misc.isUnremovable(member.getCaptain())) {
            if (member.getCaptain().isAICore()) {
                playerCargo.addCommodity(member.getCaptain().getAICoreId(), 1f);
            }
        }

        for (String slotId : variant.getNonBuiltInWeaponSlots()) {
            final String weaponId = variant.getWeaponId(slotId);
            if (weaponId != null) playerCargo.addWeapons(weaponId, 1);
        }

        for (String wingId : variant.getNonBuiltInWings()) {
            playerCargo.addFighters(wingId, 1);
        }

        playerCargo.removeCommodity(Commodities.CREW, ShipTypeData.getCrewPerShip(member.getHullSpec()));

        grantTransferBonusXp(variant);

        variant.clear();
        final FleetDataAPI fleet = member.getFleetData();

        fleet.removeFleetMember(member);
        fleet.syncMemberLists();

        RolfLectionUtil.getMethodAndInvokeDirectly("recreateUI", fleetList, true);
        Global.getSoundPlayer().playUISound("ui_cargo_special_military_drop", 1f, 1f);
    }

    private static final void grantTransferBonusXp(final ShipVariantAPI variant) {
        final LevelupPlugin plugin = Global.getSettings().getLevelupPlugin();
        
        final float totalFraction = getBonusXpFraction(variant);
        
        final MutableCharacterStatsAPI stats = Global.getSector().getPlayerStats();
        final int currentLevel = stats.getLevel();
        final long xpForNextLevel = plugin.getXPForLevel(currentLevel + 1) - plugin.getXPForLevel(currentLevel);
        
        long bonusXp = (long)(xpForNextLevel * totalFraction / plugin.getStoryPointsPerLevel());
        if (bonusXp > 0) {
            stats.addBonusXP(bonusXp, true, null, true);
        }
    }

    private static final float getBonusXpFraction(final ShipVariantAPI variant) {
        final List<String> sModIds = new ArrayList<>(variant.getSMods());
        sModIds.addAll(variant.getSModdedBuiltIns());

        float totalFraction = 0f;
        for (String modId : sModIds) {
            final HullModSpecAPI spec = Global.getSettings().getHullModSpec(modId);
            totalFraction += variant.getSModdedBuiltIns().contains(modId) ?
                0f : 1f - Misc.getBuildInBonusXP(spec, variant.getHullSize());
        }

        return totalFraction;
    }
}