package org.zzy.lib.bettercamera.preview.impl;

import android.content.Context;
import android.view.ViewGroup;

import org.zzy.lib.bettercamera.bean.Size;
import org.zzy.lib.bettercamera.preview.CameraPreview;
import org.zzy.lib.bettercamera.preview.CameraPreviewCallback;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public abstract class BaseCameraPreview implements CameraPreview {

    private int width;

    private int height;

    private CameraPreviewCallback cameraPreviewCallback;

    BaseCameraPreview(Context context,ViewGroup parent){

    }

    @Override
    public void setCameraPreviewCallback(CameraPreviewCallback cameraPreviewCallback) {
        this.cameraPreviewCallback = cameraPreviewCallback;
    }

    @Override
    public boolean isAvailable() {
        return width > 0 && height > 0;
    }

    @Override
    public Size getSize() {
        return Size.of(width, height);
    }


    void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    void notifyPreviewAvailable() {
        if (cameraPreviewCallback != null) {
            cameraPreviewCallback.onAvailable(this);
        }
    }
}
