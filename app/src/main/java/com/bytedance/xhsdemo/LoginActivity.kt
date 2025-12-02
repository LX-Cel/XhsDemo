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

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private val viewModel: LoginViewModel by viewModels {
        val dao = AppDatabase.getInstance(applicationContext).userDao()
        val session = SessionManager(applicationContext)
        LoginViewModelFactory(LoginRepository(dao, session))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)
        AppCompatDelegate.setDefaultNightMode(sessionManager.getThemeMode())
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

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            binding.statusBarSpacer.layoutParams.height = top
            binding.statusBarSpacer.requestLayout()
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            ToastUtils.cancel()
        }
        return super.dispatchTouchEvent(ev)
    }
}
