package com.subalpine;

import android.graphics.drawable.Drawable;
import android.view.ContextMenu;
import android.view.View;

import org.robolectric.fakes.RoboMenu;

/**
 * Created by robin on 12/20/15.
 */
public class RoboContextMenu extends RoboMenu implements ContextMenu {
    @Override
    public ContextMenu setHeaderTitle(int titleRes) {
        return null;
    }

    @Override
    public ContextMenu setHeaderTitle(CharSequence title) {
        return null;
    }

    @Override
    public ContextMenu setHeaderIcon(int iconRes) {
        return null;
    }

    @Override
    public ContextMenu setHeaderIcon(Drawable icon) {
        return null;
    }

    @Override
    public ContextMenu setHeaderView(View view) {
        return null;
    }

    @Override
    public void clearHeader() {

    }
}
