package wfg.ltv_econ.constants;

import static wfg.native_ui.util.Globals.settings;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;

public class Sprites {
    public static final SpriteAPI CRATES = settings.getSprite("icons", "cargo_crates");
    public static final SpriteAPI SUPPLIES = settings.getSprite(settings.getCommoditySpec(Commodities.SUPPLIES).getIconName());
    public static final SpriteAPI FUEL = settings.getSprite(settings.getCommoditySpec(Commodities.FUEL).getIconName());
    public static final SpriteAPI CREW = settings.getSprite(settings.getCommoditySpec(Commodities.CREW).getIconName());
    public static final SpriteAPI SHIPS = settings.getSprite(settings.getCommoditySpec(Commodities.SHIPS).getIconName());
    public static final SpriteAPI BERTH = settings.getSprite("icons", "berth");
    public static final SpriteAPI COMBAT = settings.getSprite("ui", "icon_kinetic");
    public static final SpriteAPI ARROW = settings.getSprite("ui", "arrow");
    public static final SpriteAPI SMUGGLING = settings.getSprite("icons", "smuggling");
    public static final SpriteAPI WAGES = settings.getSprite("icons", "ratio_chart");
    public static final SpriteAPI STOPWATCH = settings.getSprite("icons", "stopwatch");
    public static final SpriteAPI CHECKLIST = settings.getSprite("icons", "checklist");
    public static final SpriteAPI LOGISTICS = settings.getSprite("commodity_markers", "imports");
    public static final SpriteAPI HEALTHCARE = settings.getSprite("icons", "healthcare");
    public static final SpriteAPI SECURITY = settings.getSprite("simulator", "behavior_defensive");
    public static final SpriteAPI PUBLIC_INFO = settings.getSprite("icons", "radio_tower");
}