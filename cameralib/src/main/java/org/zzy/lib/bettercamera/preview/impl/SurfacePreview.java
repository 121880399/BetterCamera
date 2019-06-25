package org.zzy.lib.bettercamera.preview.impl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import org.zzy.lib.bettercamera.constant.PreviewConstant;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public class SurfacePreview extends BaseCameraPreview{

    private SurfaceHolder surfaceHolder;
    private SurfaceView surfaceView;

    public SurfacePreview(Context context, ViewGroup parent) {
        super(context, parent);
        surfaceView = new SurfaceView(context);
        surfaceView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        parent.addView(surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setKeepScreenOn(true);
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                updateSurfaceTexture(holder, width, height);
                notifyPreviewAvailable();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                updateSurfaceTexture(holder, 0, 0);
            }
        });
    }

    private void updateSurfaceTexture(SurfaceHolder surfaceHolder, int width, int height) {
        this.surfaceHolder = surfaceHolder;
        setSize(width, height);
    }

    @Override
    public Surface getSurface() {
        return surfaceHolder.getSurface();
    }

    @Override
    public int getPreviewType() {
        return PreviewConstant.SURFACE_VIEW;
    }

    @Nullable
    @Override
    public SurfaceHolder getSurfaceHolder() {
        return surfaceHolder;
    }

    @Nullable
    @Override
    public SurfaceTexture getSurfaceTexture() {
        return null;
    }

    @Override
    public View getView() {
        return surfaceView;
    }
}
