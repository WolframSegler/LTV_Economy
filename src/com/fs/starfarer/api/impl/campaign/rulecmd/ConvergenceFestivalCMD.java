package com.fs.starfarer.api.impl.campaign.rulecmd;

import static wfg.native_ui.util.Globals.settings;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.InteractionDialogImageVisual;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.missions.BarCMD;
import com.fs.starfarer.api.impl.campaign.rulecmd.missions.BarCMD.BarAmbiencePlayer;
import com.fs.starfarer.api.util.Misc.Token;

public class ConvergenceFestivalCMD extends BaseCommandPlugin {
    private static final String ILLUSTRATION = "graphics/illustrations/convergence_festival.jpg";
    private static final String SPACE_BAR = "graphics/illustrations/space_bar.jpg";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        final String signal = params.get(0).getString(memoryMap);
        final BarAmbiencePlayer ambience = BarCMD.getAmbiencePlayer();
        final SoundPlayerAPI player = Global.getSoundPlayer();

        switch (signal) {
        case "kill_music":
            player.pauseMusic();
            break;

        case "start_daze":
            if (ambience != null) ambience.volume = 0.3f;
            player.setSuspendDefaultMusicPlayback(true);
            player.playCustomMusic(1, 1, "music_convergence_festival", true);
            try {
                settings.loadTexture(ILLUSTRATION);
                dialog.getVisualPanel().showImageVisual(new InteractionDialogImageVisual(ILLUSTRATION, 480, 300));
            } catch (Exception e) {}
            break;

        case "end_daze":
            if (ambience != null) ambience.volume = 1f;
            player.pauseCustomMusic();
            player.setSuspendDefaultMusicPlayback(false);
		    player.restartCurrentMusic();
            dialog.getVisualPanel().showImageVisual(new InteractionDialogImageVisual(SPACE_BAR, 480, 300));
            break;
        }
	
		return true;
	}
}