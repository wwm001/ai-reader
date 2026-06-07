package com.example.chatgptreader;

import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class SpeedTileService extends TileService {
    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        ReaderSettingsRepository.cycleSpeechRate(this);
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        tile.setState(Tile.STATE_ACTIVE);
        String rate = ReaderSettingsRepository.formatRate(ReaderSettingsRepository.getSpeechRate(this));
        if (Build.VERSION.SDK_INT >= 29) {
            tile.setSubtitle(rate);
        }
        if (Build.VERSION.SDK_INT >= 30) {
            tile.setStateDescription(rate);
        }
        tile.updateTile();
    }
}
