package org.floens.chan.ui.layout;

import static org.floens.chan.Chan.inject;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import org.floens.chan.R;
import org.floens.chan.core.manager.ArchivesManager;
import org.floens.chan.core.model.PostLinkable;
import org.floens.chan.core.model.orm.Loadable;

public class ArchivesLayout extends LinearLayout {
    private Callback callback;
    private ArrayAdapter<Pair<String, String>> adapter;
    private Loadable op;
    private PostLinkable.ThreadLink threadLink;
    private AlertDialog alertDialog;

    public ArchivesLayout(Context context) {
        this(context, null);
    }

    public ArchivesLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ArchivesLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        inject(this);
        op = Loadable.emptyLoadable();
        threadLink = null;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1);
        ((ListView) findViewById(R.id.archives_list)).setAdapter(adapter);
        ((ListView) findViewById(R.id.archives_list)).setOnItemClickListener((parent, view, position, id) -> {
            String link;
            if (threadLink != null) {
                link = "https://" + ((Pair) parent.getItemAtPosition(position)).second + "/" + threadLink.board + "/post/" + threadLink.postId + "/";
            } else {
                link = "https://" + ((Pair) parent.getItemAtPosition(position)).second + "/" + op.boardCode + "/thread/" + op.no + "/";
            }
            callback.openLinkInBrowser(link);
            if (alertDialog != null) alertDialog.dismiss();
        });
    }

    public void setCallback(Callback c) {
        callback = c;
    }

    public boolean setLoadable(@NonNull Loadable op) {
        this.op = op;
        adapter.addAll(ArchivesManager.getInstance().archivesForBoard(op.board));
        return !adapter.isEmpty();
    }

    public boolean setThreadLink(@NonNull PostLinkable.ThreadLink threadLink) {
        this.threadLink = threadLink;
        adapter.addAll(ArchivesManager.getInstance().archivesFor4ChanBoard(threadLink.board));
        return !adapter.isEmpty();
    }

    public void attachToDialog(AlertDialog alertDialog) {
        this.alertDialog = alertDialog;
    }

    public interface Callback {
        void openLinkInBrowser(String link);
    }
}
