package org.zzy.lib.bettercamera.config.creator.impl;

import android.content.Context;
import android.os.Build;
import android.view.ViewGroup;

import org.zzy.lib.bettercamera.config.creator.CameraPreviewCreator;
import org.zzy.lib.bettercamera.preview.CameraPreview;
import org.zzy.lib.bettercamera.preview.impl.SurfacePreview;
import org.zzy.lib.bettercamera.preview.impl.TexturePreview;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public class CameraPreviewCreatorImpl implements CameraPreviewCreator {

    /**
     * 大于等于安卓4.0使用TexturePreview
     */
    @Override
    public CameraPreview create(Context context, ViewGroup parent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return new TexturePreview(context, parent);
        }
        return new SurfacePreview(context, parent);
    }
}
