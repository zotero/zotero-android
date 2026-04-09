package org.zotero.android.screens.htmlepub.reader.web;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

public class HtmlEpubReaderWevViewSelectActionModeCallback implements ActionMode.Callback {

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu.clear();
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }
}