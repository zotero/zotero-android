package org.zotero.android.screens.htmlepub.reader.web;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import org.zotero.android.R;

public class HtmlEpubReaderWevViewSelectActionModeCallback implements ActionMode.Callback {

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.contextual_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_custom_one) {
            mode.finish();
            return true;
        } else if (id == R.id.action_custom_two) {
            mode.finish();
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }
}