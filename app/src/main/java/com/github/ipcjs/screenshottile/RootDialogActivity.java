package com.github.ipcjs.screenshottile;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import static com.github.ipcjs.screenshottile.Utils.hasRoot;
import static com.github.ipcjs.screenshottile.Utils.p;

/**
 * Created by ipcjs on 02/07.
 */

public class RootDialogActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            RootDialogFragment fragment = RootDialogFragment.newInstance();
//            fragment.setCancelable(false);
            fragment.show(getFragmentManager(), RootDialogFragment.class.getName());
        }
    }

    public static class RootDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
        public static RootDialogFragment newInstance() {
            return new RootDialogFragment();
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
                        startActivity(new Intent(getActivity(), RootDialogActivity.class));
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
}
