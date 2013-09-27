package de.blinkt.openvpn;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.*;
import android.widget.SpinnerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by arne on 22.09.13.
 */
public class LogDetailActionProvider extends ActionProvider {
    /**
     * Creates a new instance. ActionProvider classes should always implement a
     * constructor that takes a single Context parameter for inflating from menu XML.
     *
     * @param context Context for accessing resources.
     */
    public LogDetailActionProvider(Context context) {
        super(context);
    }

    @Override
    public View onCreateActionView() {
        return null;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public View onCreateActionView(MenuItem forItem) {
        return super.onCreateActionView(forItem);
    }

    @Override
    public boolean hasSubMenu() {
        return true;
    }

    @Override
    public void onPrepareSubMenu(SubMenu subMenu) {
        subMenu.add(1, Menu.NONE, Menu.NONE, "one");
        subMenu.add(1, Menu.NONE, Menu.NONE, "two");
        subMenu.add(1, Menu.NONE, Menu.NONE, "three");
        subMenu.add(1, Menu.NONE, Menu.NONE, "four");

        subMenu.add(2, Menu.NONE, Menu.NONE, "no");
        subMenu.add(2, Menu.NONE, Menu.NONE, "short");
        subMenu.add(2, Menu.NONE, Menu.NONE, "long");


    }

    @Override
    public boolean onPerformDefaultAction() {
        return super.onPerformDefaultAction();
    }
}