package org.zzy.lib.bettercamera.config.creator;

import android.content.Context;
import android.view.ViewGroup;

import org.zzy.lib.bettercamera.preview.CameraPreview;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public interface CameraPreviewCreator {

    /**
     * 用于创建CameraPreview
     */
    CameraPreview create(Context context, ViewGroup parent);
}
