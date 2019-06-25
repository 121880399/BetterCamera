package org.zzy.lib.bettercamera.preview.impl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import org.zzy.lib.bettercamera.constant.PreviewConstant;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public class TexturePreview extends BaseCameraPreview{

    private SurfaceTexture surfaceTexture;
    private TextureView textureView;

    public TexturePreview(Context context, ViewGroup parent) {
        super(context, parent);
        textureView = new TextureView(context);
        textureView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        parent.addView(textureView);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                updateSurfaceTexture(surface, width, height);
                notifyPreviewAvailable();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                updateSurfaceTexture(surface, width, height);
                notifyPreviewAvailable();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                updateSurfaceTexture(surface, 0, 0);
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }
    private void updateSurfaceTexture(SurfaceTexture surfaceTexture, int width, int height) {
        this.surfaceTexture = surfaceTexture;
        setSize(width, height);
    }


    @Override
    public Surface getSurface() {
        return new Surface(surfaceTexture);
    }

    @Override
    public int getPreviewType() {
        return PreviewConstant.TEXTURE_VIEW;
    }

    @Nullable
    @Override
    public SurfaceHolder getSurfaceHolder() {
        return null;
    }

    @Nullable
    @Override
    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    @Override
    public View getView() {
        return textureView;
    }
}
