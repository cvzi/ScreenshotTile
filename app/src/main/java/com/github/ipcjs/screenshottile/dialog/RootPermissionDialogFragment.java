package com.github.ipcjs.screenshottile.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.github.ipcjs.screenshottile.R;

import static com.github.ipcjs.screenshottile.Utils.hasRoot;
import static com.github.ipcjs.screenshottile.Utils.p;

/**
 * Created by ipcjs on 2017/8/16.
 */
public class RootPermissionDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
    public static RootPermissionDialogFragment newInstance() {
        return new RootPermissionDialogFragment();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity(), getTheme())
                .setTitle(R.string.dialog_obtain_root)
                .setMessage(getString(R.string.dialog_obtain_root_message, getString(R.string.app_name)))
                .setPositiveButton(R.string.dialog_reacquire, this)
                .setNeutralButton(R.string.dialog_i_know, this)
                .setNegativeButton(R.string.dialog_uninstall, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case Dialog.BUTTON_POSITIVE:
                if (!hasRoot()) {
                    TransparentContainerActivity.Companion.start(getActivity(), RootPermissionDialogFragment.class, null);
                }
                break;
            case Dialog.BUTTON_NEUTRAL:
                break;
            case Dialog.BUTTON_NEGATIVE:
                startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:" + getActivity().getPackageName())));
                break;
        }
        getActivity().finish();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        p("RootDialogFragment.onDismiss");
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        getActivity().finish();
    }
}
