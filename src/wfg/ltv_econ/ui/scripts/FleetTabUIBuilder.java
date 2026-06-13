package wfg.ltv_econ.ui.scripts;

import static wfg.native_ui.util.UIConstants.pad;

import java.util.List;

import com.fs.graphics.util.Fader;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.fleet.FleetMember;

import rolflectionlib.util.RolfLectionUtil;
import wfg.ltv_econ.ui.fleetTab.button.TransferToFactionInventoryBtn;
import wfg.ltv_econ.ui.reusable.IdentityMarker;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;

public class FleetTabUIBuilder implements CoreTabUIBuilder {
    
    @SuppressWarnings("unchecked")
    public void advance(float delta) {
        final MarketAPI market = Global.getSector().getCurrentlyOpenMarket();
        if (market == null || !market.isPlayerOwned()) return;

        final UIPanelAPI masterTab = Attachments.getCurrentTab();
        if (masterTab == null) return;
    
        final UIPanelAPI fleetPanel = (UIPanelAPI) RolfLectionUtil.getMethodAndInvokeDirectly("getFleetPanel", masterTab);
        if (fleetPanel == null) return;

        final FleetDataAPI fleetData = (FleetDataAPI) RolfLectionUtil.getMethodAndInvokeDirectly("getFleetData", fleetPanel);
        if (fleetData == null || fleetData.getFleet() == null || !fleetData.getFleet().isPlayerFleet()) return;

        final UIPanelAPI fleetList = (UIPanelAPI) RolfLectionUtil.getMethodAndInvokeDirectly("getList", fleetPanel);
        final List<UIComponentAPI> widgets = (List<UIComponentAPI>) RolfLectionUtil.getMethodAndInvokeDirectly("getItems", fleetList);

        boolean hasCheckedIdentityPresence = false;

        for (UIComponentAPI widgetObj : widgets) {
            final UIPanelAPI widget = (UIPanelAPI) widgetObj;

            if (!hasCheckedIdentityPresence) {
                if (IdentityMarker.isPresent(widget)) return;
                hasCheckedIdentityPresence = true;
            }
            IdentityMarker.attach(widget);

            final List<?> widgetChildren = (List<?>) RolfLectionUtil.invokeMethodDirectly(CustomPanel.getChildrenNonCopyMethod, widget);

            final UIPanelAPI buttonsPanel = widgetChildren.stream()
                .filter(c -> RolfLectionUtil.hasMethodOfName("updateTooltip", c))
                .map(child -> (UIPanelAPI) child).findFirst().orElse(null);
            if (buttonsPanel == null) continue;
            final List<ButtonAPI> buttons = (List<ButtonAPI>) RolfLectionUtil.invokeMethodDirectly(CustomPanel.getChildrenNonCopyMethod, buttonsPanel);
            if (buttons == null) continue;

            final Fader widgetFader = (Fader) RolfLectionUtil.getMethodAndInvokeDirectly("getHoverFader", widget);
            final FleetMemberAPI member = (FleetMember) RolfLectionUtil.getMethodAndInvokeDirectly("getMember", widget);
            final var btn = new TransferToFactionInventoryBtn(widget, widgetFader, member, fleetPanel);

            widget.addComponent(btn.getPanel());
            NativeUiUtils.anchorPanel(btn.getPanel(), buttons.get(buttons.size() - 1), AnchorType.LeftMid, pad);
        }
    }
}