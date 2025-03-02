package com.billiecamera.camera

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.billiecamera.camera.listener.ClickListener
import com.billiecamera.camera.listener.FlowCameraListener
import com.billiecamera.camera.utils.FileUtils
import com.billiecamera.camera.view.ButtonState
import com.billiecamera.camera.view.CaptureButton.Companion.BUTTON_STATE_BOTH
import com.billiecamera.camera.view.CaptureButton.Companion.BUTTON_STATE_ONLY_CAPTURE
import com.billiecamera.camera.view.CaptureButton.Companion.BUTTON_STATE_ONLY_RECORDER
import com.billiecamera.camera.view.ResModel
import com.google.gson.Gson
import com.gyf.immersionbar.BarHide
import com.gyf.immersionbar.ktx.immersionBar
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.reactnativebilliecamera.R
import com.reactnativebilliecamera.databinding.CameraActivityBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class CameraActivity  : AppCompatActivity() {

    /**
     * result model
     */
    private var resModel:ResModel? = null

    /**
     * result callBack
     */
    private var callBack:((res:String)->Unit)? = null

    /**
     * binding view
     */
    private lateinit var binding: CameraActivityBinding

    /**
     * can capture video
     */
    private var videoEnable = 0

    private val placeView:FrameLayout by lazy {
        FrameLayout(this@CameraActivity).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    companion object {
        // select result callBack
        private var sCallBack:((res:String)->Unit)? = null
        private var videoEnableKey = "videoEnable"

        var hasOpenCamera = false

        // start Activity
        fun startCamera(context:Activity, videoEnable:Int, callBack:((res:String)->Unit)) {
            if(hasOpenCamera) {
                return
            }
            try {
                val intent = Intent(context, CameraActivity::class.java)
                intent.putExtra(videoEnableKey, videoEnable)
                context.startActivity(intent)
                sCallBack = callBack
                hasOpenCamera = true
            } catch (e:Throwable) {
                e.printStackTrace()
                hasOpenCamera = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        callBack = sCallBack
        sCallBack = null

        setImmersionBar()
        // capture model
        videoEnable = intent.getIntExtra(videoEnableKey, BUTTON_STATE_BOTH)

        setContentView(placeView)

        XXPermissions.with(this)
            // request single permission
            .permission(Permission.RECORD_AUDIO)
            // request single permission
            .permission(Permission.CAMERA)
            // request interceptor
            //.interceptor(new PermissionInterceptor())
            // 设置不触发错误检测机制（局部设置）
            //.unchecked()
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    if (allGranted) {
                        grantedCallBack()
                    } else {
                        // not all grand
                        Toast.makeText(this@CameraActivity,
                            "request camera permission", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    if (doNotAskAgain) {
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        XXPermissions.startPermissionActivity(this@CameraActivity, permissions)
                    } else {
                        // not all grand
                        Toast.makeText(this@CameraActivity,
                            "request camera permission", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            })
    }

    private fun grantedCallBack() {
        binding = CameraActivityBinding.inflate(layoutInflater, placeView, true)

        // 绑定生命周期 您就不用关心Camera的开启和关闭了 不绑定无法预览
        binding.flowCamera.initCamera(this, this)

        // 设置白平衡模式
        // flowCamera.setWhiteBalance(WhiteBalance.AUTO)

        // 设置只支持单独拍照拍视频还是都支持
        // BUTTON_STATE_ONLY_CAPTURE  BUTTON_STATE_ONLY_RECORDER  BUTTON_STATE_BOTH
        binding.flowCamera.setCaptureMode(ButtonState.getButtonState(videoEnable) ?: ButtonState.BUTTON_STATE_ONLY_CAPTURE)

        // 开启HDR
        // flowCamera.setHdrEnable(Hdr.ON)

        // 设置最大可拍摄小视频时长
        binding.flowCamera.setRecordVideoMaxTime(10)
        // 设置拍照或拍视频回调监听
        binding.flowCamera.setFlowCameraListener(object : FlowCameraListener {
            // 录制完成视频文件返回
            override fun success(file: ResModel) {
                resModel = file
                callResult()
            }
            // 操作拍照或录视频出错
            override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                Toast.makeText(this@CameraActivity, message, Toast.LENGTH_LONG).show()
                finish()
            }
        })
        binding.flowCamera.setOnClickListener {

        }
        //左边按钮点击事件
        binding.flowCamera.setLeftClickListener(object : ClickListener {
            override fun onClick() {
                finish()
            }
        })
        binding.flowCamera.setGalleryClickListener(object : ClickListener {
            override fun onClick() {
                openGallery()
            }
        })
    }

    /**
     * 设置沉浸式状态栏
     */
    private fun setImmersionBar() {
        immersionBar {
            statusBarDarkFont(true, 0.2f)
            navigationBarColor(R.color.white)
            hideBar(BarHide.FLAG_HIDE_NAVIGATION_BAR)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hasOpenCamera = false
        callBack = null
    }

    /**
     * open Gallery
     */
    private fun openGallery() {
        XXPermissions.with(this)
            .permission(Permission.READ_MEDIA_VISUAL_USER_SELECTED)
            .permission(Permission.READ_MEDIA_VIDEO)
            .permission(Permission.READ_MEDIA_IMAGES)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    if (allGranted) {
                        val intent = Intent(Intent.ACTION_PICK)
                        when (videoEnable) {
                            BUTTON_STATE_BOTH -> {
                                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/* video/*")
                            }
                            BUTTON_STATE_ONLY_RECORDER -> {
                                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "video/*")
                            }
                            else -> {
                                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                            }
                        }
                        getContent.launch(intent)
                    } else {
                        // not all grand
                        Toast.makeText(this@CameraActivity,
                            "request camera permission", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    if (doNotAskAgain) {
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        XXPermissions.startPermissionActivity(this@CameraActivity, permissions)
                    } else {
                        // not all grand
                        Toast.makeText(this@CameraActivity,
                            "request camera permission", Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    /**
     * get video or image result
     */
    private val getContent = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode != Activity.RESULT_OK) {
            return@registerForActivityResult
        }
        lifecycleScope.launch {
            val resultIntent = result.data
            resModel = ResModel()
            if(result.data?.type?.contains("video") == true) {
                // read video info
                FileUtils.getMediaInfo(this@CameraActivity, resModel!!, resultIntent?.data)
            } else if(result.data?.type?.contains("image") == true) {
                // read picture info
                FileUtils.getImageInfo(this@CameraActivity, resModel!!, resultIntent?.data)
            }
            // select null file
            if(resModel?.uri == null) {
                return@launch
            }
            callResult()
        }
    }

    private fun callResult() {
        val model = resModel ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            val gson = Gson()
            val json = gson.toJson(model)
            callBack?.invoke(json)

            finish()
        }
    }
}
