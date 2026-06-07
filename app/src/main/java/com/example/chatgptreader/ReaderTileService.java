package com.example.chatgptreader;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.os.Build;

public class ReaderTileService extends TileService {
    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        if (!AccessibilityStatus.isServiceEnabled(this)) {
            ReaderState.setMode(this, ReaderMode.OFF);
            ReaderCommandBus.send(this, ReaderCommandBus.COMMAND_OFF);
        } else {
            boolean enable = !ReaderState.isReaderEnabled(this);
            ReaderState.setMode(this, enable ? ReaderMode.ON : ReaderMode.OFF);
            ReaderCommandBus.send(this, enable ? ReaderCommandBus.COMMAND_ON : ReaderCommandBus.COMMAND_OFF);
        }
        ReaderNotificationController.update(this);
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        if (!AccessibilityStatus.isServiceEnabled(this)) {
            tile.setState(Tile.STATE_UNAVAILABLE);
            setSubtitle(tile, "AccessibilityService 未設定");
        } else if (ReaderState.getMode(this) == ReaderMode.ON) {
            tile.setState(Tile.STATE_ACTIVE);
            setSubtitle(tile, "有効");
        } else if (ReaderState.getMode(this) == ReaderMode.PAUSED) {
            tile.setState(Tile.STATE_INACTIVE);
            setSubtitle(tile, "一時停止");
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            setSubtitle(tile, "無効");
        }
        tile.updateTile();
    }

    private void setSubtitle(Tile tile, String subtitle) {
        if (Build.VERSION.SDK_INT >= 29) {
            tile.setSubtitle(subtitle);
        }
    }
}
