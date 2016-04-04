package org.kore.kolabnotes.android.fragment;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.kore.kolabnotes.android.ColorCircleDrawable;
import org.kore.kolabnotes.android.DrawEditorActivity;
import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.Utils;
import org.kore.kolabnotes.android.draweditor.DrawingView;
import org.kore.kolabnotes.android.draweditor.ToolButton;
import org.kore.kolabnotes.android.draweditor.ToolButtonGroup;

import java.io.ByteArrayOutputStream;

import yuku.ambilwarna.AmbilWarnaDialog;

/**
 * Created by yaroslav on 25.03.16.
 */

public class DrawEditorFragment extends Fragment {
    private static final String TAG_COLOR = "color";
    private static final String TAG_SIZE = "size";
    private static final String TAG_IS_ERASER = "isEraser";

    private static final int BRUSH_ALPHA = 128;
    private static final int MAX_BRUSH_SIZE = 100;

    private AppCompatActivity activity;
    private Toolbar toolbar;
    private DrawingView mCanvas;
    private LinearLayout mToolsOptions;
    private ToolButtonGroup mTools;
    private ToolButton mScaleModeButton;
    private ImageButton mBrushColor;
    private int mColor = DrawingView.DEFAULT_BRUSH_COLOR;
    private int mSize = DrawingView.DEFAULT_BRUSH_SIZE;
    private boolean isEraser = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_draweditor, container, false);
        mCanvas = (DrawingView) v.findViewById(R.id.draweditor);
        mTools = (ToolButtonGroup) v.findViewById(R.id.draweditor_tools);
        mToolsOptions = (LinearLayout) v.findViewById(R.id.draweditor_tools_options);
        mBrushColor = (ImageButton) v.findViewById(R.id.draweditor_color);
        mScaleModeButton = (ToolButton) v.findViewById(R.id.draweditor_scaling);

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof AppCompatActivity) {
            this.activity = (AppCompatActivity)context;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar_draweditor);

        if (activity == null) {
            activity = (AppCompatActivity) getActivity();
        }
        activity.setSupportActionBar(toolbar);
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setHasOptionsMenu(true);

        mCanvas.setListener(new DrawingView.DrawingListener() {
            @Override
            public void onDrawEvent() {
                activity.invalidateOptionsMenu();
            }
        });

        if (mTools != null)
            mTools.setOnCheckedChangeListener(new ToolButtonGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(ToolButtonGroup group, @IdRes int checkedId) {
                    isEraser = false;
                    mBrushColor.setImageDrawable(new ColorCircleDrawable(mColor,
                            R.color.theme_selected_notes));
                    switch (checkedId) {
                        case R.id.draweditor_pen:
                            mCanvas.setBrush(mColor, mSize, DrawingView.DEFAULT_BRUSH_ALPHA);
                            break;
                        case R.id.draweditor_marker_pen:
                            mCanvas.setBrush(mColor, mSize, BRUSH_ALPHA);
                            break;
                        case R.id.draweditor_eraser:
                            /* Disabling color chooser for erase brush */
                            isEraser = true;
                            mBrushColor.setImageDrawable(new ColorCircleDrawable(Color.TRANSPARENT,
                                    R.color.theme_selected_notes));
                            mCanvas.setBrush(mCanvas.getCanvasColor(), mSize, DrawingView.DEFAULT_BRUSH_ALPHA);
                            break;
                    }
                }
            });

        if (savedInstanceState != null) {
            mColor = savedInstanceState.getInt(TAG_COLOR);
            mSize = savedInstanceState.getInt(TAG_SIZE);
            isEraser = savedInstanceState.getBoolean(TAG_IS_ERASER);
        }

        Utils.setElevation(mToolsOptions, 30);
        mBrushColor.setImageDrawable(new ColorCircleDrawable(mColor,
                R.color.theme_selected_notes));

        mBrushColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isEraser) {
                    chooseColor();
                }
            }
        });

        activity.findViewById(R.id.draweditor_line_weight).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = activity.getLayoutInflater();
                View view = inflater.inflate(R.layout.dialog_seekbar_draweditor, null);
                AlertDialog dialog = brushSizeDialog(view);
                dialog.show();
            }
        });

        mScaleModeButton.setMode(ToolButton.ToolButtonMode.MODE_CHECK_BOX);
        mScaleModeButton.setOnCheckedChangeListener(new ToolButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(ToolButton button, boolean isChecked) {
                mCanvas.setScaleMode(isChecked);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(TAG_COLOR, mColor);
        outState.putInt(TAG_SIZE, mSize);
        outState.putBoolean(TAG_IS_ERASER, isEraser);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (isEraser) {
            mBrushColor.setImageDrawable(new ColorCircleDrawable(Color.TRANSPARENT,
                    R.color.theme_selected_notes));
        } else {
            mBrushColor.setImageDrawable(new ColorCircleDrawable(mColor,
                    R.color.theme_selected_notes));
        }
    }

    private AlertDialog brushSizeDialog(View v) {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(activity);

        dialog.setTitle(R.string.draweditor_title_line_weight);
        dialog.setView(v);

        final TextView counter = (TextView) v.findViewById(R.id.draweditor_counter);
        counter.setText(Integer.toString(mSize));

        final int[] brushSize = {-1};

        final SeekBar seekBar = (SeekBar) v.findViewById(R.id.draweditor_seekbar);
        seekBar.setMax(MAX_BRUSH_SIZE);
        seekBar.setProgress(mSize);
        seekBar.setKeyProgressIncrement(1);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                brushSize[0] = progress;
                counter.setText(Integer.toString(brushSize[0]));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                /* Nothing */
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                /* Nothing */
            }
        });

        dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (brushSize[0] != -1) {
                    mSize = brushSize[0];
                }
                mCanvas.setBrushSize(mSize);
            }
        });
        dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                /* Nothing */
            }
        });

        return dialog.create();
    }

    @Nullable
    private byte[] saveToBitmap() {
        /* Compressing original bitmap */
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Bitmap bitmap = mCanvas.getBitmap();
        if (bitmap != null) {
            Toast.makeText(activity, R.string.draweditor_saving_toast, Toast.LENGTH_SHORT).show();
            /* Scaling for editor */
            int width = bitmap.getWidth() / 3;
            int height = bitmap.getHeight() / 3;

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);

            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

            return out.toByteArray();
        }

        return null;
    }

    void chooseColor() {
        final int initialColor = mCanvas.getBrushColor();

        AmbilWarnaDialog dialog = new AmbilWarnaDialog(activity, initialColor, true, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                mColor = color;
                mCanvas.setBrushColor(color);
                mBrushColor.setImageDrawable(new ColorCircleDrawable(color,
                        R.color.theme_selected_notes));
            }

            @Override
            public void onRemove(AmbilWarnaDialog dialog) {
                mColor = DrawingView.DEFAULT_BRUSH_COLOR;
                mCanvas.setBrushColor(DrawingView.DEFAULT_BRUSH_COLOR);
                mBrushColor.setImageDrawable(new ColorCircleDrawable(DrawingView.DEFAULT_BRUSH_COLOR,
                        R.color.theme_selected_notes));
            }

            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
                    /* Nothing */
            }
        });
        dialog.show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.draweditor_toolbar, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem undo = menu.findItem(R.id.undo_menu);
        MenuItem redo = menu.findItem(R.id.redo_menu);
        Drawable iconUndo = ContextCompat.getDrawable(activity, R.drawable.ic_undo_white_36dp);
        Drawable iconRedo = ContextCompat.getDrawable(activity, R.drawable.ic_redo_white_36dp);

        if (mCanvas.getLinesCount() == 0) {
            iconUndo.setColorFilter(ContextCompat.getColor(activity, R.color.theme_default_primary_light),
                    PorterDuff.Mode.SRC_IN);
        } else {
            iconUndo.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        }
        if (mCanvas.getUndoneLinesCount() == 0) {
            iconRedo.setColorFilter(ContextCompat.getColor(activity, R.color.theme_default_primary_light),
                    PorterDuff.Mode.SRC_IN);
        } else {
            iconRedo.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        }

        undo.setIcon(iconUndo);
        redo.setIcon(iconRedo);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.ok_menu:
                Intent i = new Intent();
                i.putExtra(DrawEditorActivity.TAG_RETURN_BITMAP, saveToBitmap());
                ((OnFragmentCallback)activity).fragmentFinished(i, OnFragmentCallback.ResultCode.OK);
                break;
            case R.id.undo_menu:
                mCanvas.undo();
                break;
            case R.id.redo_menu:
                mCanvas.redo();
                break;
            case R.id.clear_menu:
                mCanvas.clearCanvas();
                break;
        }
        return true;
    }

    public void onBackPressed() {
        if (mCanvas.getLinesCount() != 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            builder.setTitle(R.string.dialog_cancel_warning);
            builder.setMessage(R.string.dialog_question_cancel);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    goBack();
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    /* Nothing */
                }
            });
            builder.show();
        } else {
            goBack();
        }
    }

    public void goBack() {
        ((OnFragmentCallback)activity).fragmentFinished(new Intent(), OnFragmentCallback.ResultCode.BACK);
    }
}
