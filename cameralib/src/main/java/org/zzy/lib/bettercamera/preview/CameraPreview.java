package org.zzy.lib.bettercamera.preview;

import android.graphics.SurfaceTexture;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;

import org.zzy.lib.bettercamera.bean.Size;
import org.zzy.lib.bettercamera.constant.Preview;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public interface CameraPreview {

    /**
     * 相机预览回调
     */
    void setCameraPreviewCallback(CameraPreviewCallback cameraPreviewCallback);

    /**
     * 得到Surface
     * @return
     */
    Surface getSurface();

    /**
     * 得到预览类型
     */
    @Preview.Type
    int getPreviewType();

    /**
     * 从SurfaceView得到SurfaceHoder，如果预览类型是TextureView有可能返回空
     */
    @Nullable
    SurfaceHolder getSurfaceHolder();

    /**
     * 从TextureView得到SurfaceTexture，如果预览类型是SurfaceView，可能返回空
     * @return
     */
    @Nullable
    SurfaceTexture getSurfaceTexture();

    /**
     * 预览是否可用
     */
    boolean isAvailable();

    /**
     * 得到预览的尺寸
     * @return
     */
    Size getSize();

    /**
     * 得到TextureView或者SurfaceView
     * @return
     */
    View getView();
}
