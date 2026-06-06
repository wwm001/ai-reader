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
            ReaderState.setReaderEnabled(this, false);
        } else {
            ReaderState.setReaderEnabled(this, !ReaderState.isReaderEnabled(this));
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
        } else if (ReaderState.isReaderEnabled(this)) {
            tile.setState(Tile.STATE_ACTIVE);
            setSubtitle(tile, "有効");
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
