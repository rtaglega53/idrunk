package com.uag.micros.idrunkapp.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.uag.micros.idrunkapp.R;

import java.lang.ref.WeakReference;

public class PairWithDeviceDialogFragment extends DialogFragment {
    private WeakReference<PositiveClickListener> mListenerWR;
    private int mDeviceClickedPosition = -1;

    public PairWithDeviceDialogFragment(int deviceClickedPosition) {
        mDeviceClickedPosition = deviceClickedPosition;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            mListenerWR = new WeakReference<>((PositiveClickListener) context);
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement PositiveClickListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.pair_with_device_dialog_title)
                .setPositiveButton(R.string.pair_with_device_dialog_title_positive,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final PositiveClickListener listener = mListenerWR.get();

                        if (listener != null) {
                            listener.onPositiveClickListener(mDeviceClickedPosition);
                        }
                        PairWithDeviceDialogFragment.this.getDialog().cancel();
                    }
                });

        return builder.create();
    }

    public interface PositiveClickListener {
        void onPositiveClickListener(int devicePosition);
    }
}
