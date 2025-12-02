package com.bytedance.xhsdemo

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bytedance.xhsdemo.data.LoginRepository
import com.bytedance.xhsdemo.data.SessionManager
import com.bytedance.xhsdemo.data.local.AppDatabase
import com.bytedance.xhsdemo.databinding.ActivityLoginBinding
import com.bytedance.xhsdemo.ui.login.LoginUiState
import com.bytedance.xhsdemo.ui.login.LoginViewModel
import com.bytedance.xhsdemo.ui.login.LoginViewModelFactory
import com.bytedance.xhsdemo.utils.ToastUtils
import kotlinx.coroutines.launch

// 登录页：负责用户账号密码输入、校验和跳转到主页面
class LoginActivity : AppCompatActivity() {

    // ViewBinding 引用
    private lateinit var binding: ActivityLoginBinding
    // 登录状态与主题模式管理
    private lateinit var sessionManager: SessionManager
    // 登录业务 ViewModel，使用工厂注入本地数据库 UserDao 与 SessionManager
    private val viewModel: LoginViewModel by viewModels {
        val dao = AppDatabase.getInstance(applicationContext).userDao()
        val session = SessionManager(applicationContext)
        LoginViewModelFactory(LoginRepository(dao, session))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化会话管理，并根据上次选择的主题模式应用夜间/日间主题
        sessionManager = SessionManager(this)
        AppCompatDelegate.setDefaultNightMode(sessionManager.getThemeMode())
        // 如果已经登录过，直接跳到主页面，避免重复看到登录页
        if (sessionManager.isLoggedIn()) {
            navigateToMain()
            return
        }
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets()

        setupViews()
        observeState()
    }

    // 绑定页面上所有交互控件的事件
    private fun setupViews() {
        binding.btnBack.setOnClickListener { finish() }
        binding.helpText.setOnClickListener {
            ToastUtils.show(this, getString(R.string.login_help_tip))
        }
        binding.inputPhone.addTextChangedListener { viewModel.onPhoneChanged(it?.toString().orEmpty()) }
        binding.inputPassword.addTextChangedListener { viewModel.onPasswordChanged(it?.toString().orEmpty()) }
        binding.togglePassword.setOnClickListener { viewModel.togglePasswordVisibility() }
        binding.checkboxAgreement.setOnCheckedChangeListener { _, checked ->
            viewModel.onAgreementChecked(checked)
        }
        binding.btnLogin.setOnClickListener { viewModel.submitLogin() }
        binding.switchVerifyCode.setOnClickListener {
            ToastUtils.show(this, getString(R.string.login_switch_verify))
        }
        binding.forgotPassword.setOnClickListener {
            ToastUtils.show(this, getString(R.string.login_forget_password_tip))
        }
        binding.socialWechat.setOnClickListener {
            ToastUtils.show(this, getString(R.string.login_wechat))
        }
        binding.socialQq.setOnClickListener {
            ToastUtils.show(this, getString(R.string.login_qq))
        }
        binding.socialApple.setOnClickListener {
            ToastUtils.show(this, getString(R.string.login_apple))
        }
        binding.btnAccountRecovery.setOnClickListener {
            ToastUtils.show(this, getString(R.string.login_account_recovery))
        }
    }

    // 订阅 ViewModel 状态和事件流，根据登录状态更新 UI / Toast
    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { renderState(it) }
                }
                launch {
                    viewModel.events.collect {
                        ToastUtils.show(this@LoginActivity, it)
                    }
                }
            }
        }
    }

    // 将 LoginUiState 渲染到界面上：按钮状态、错误提示、密码可见性等
    private fun renderState(state: LoginUiState) {
        binding.btnLogin.isEnabled =
            state.phone.isNotBlank() && state.password.isNotBlank() && !state.isLoading
        binding.btnLogin.text =
            if (state.isLoading) getString(R.string.login_loading) else getString(R.string.login_button)
        binding.errorText.text = state.error ?: ""
        binding.errorText.isVisible = state.error != null

        if (state.isPasswordVisible) {
            binding.inputPassword.transformationMethod = null
            binding.togglePassword.setImageResource(R.drawable.ic_eye_on)
        } else {
            binding.inputPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.togglePassword.setImageResource(R.drawable.ic_eye_off)
        }
        // 保持光标位置
        binding.inputPassword.setSelection(binding.inputPassword.text?.length ?: 0)

        if (state.loginSuccess) {
            navigateToMain()
        }
    }

    // 登录成功或已登录时跳转到主页面，并附带转场动画
    private fun navigateToMain() {
        sessionManager.setLoggedIn(true)
        startActivity(Intent(this, MainActivity::class.java))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        finish()
    }

    // 动态适配状态栏高度，填充顶部占位 View，保证沉浸式效果
    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            binding.statusBarSpacer.layoutParams.height = top
            binding.statusBarSpacer.requestLayout()
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    // 捕获全局点击事件，按下时取消正在显示的 Toast
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            ToastUtils.cancel()
        }
        return super.dispatchTouchEvent(ev)
    }
}
