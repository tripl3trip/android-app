/*
 * Copyright (c) 2020 Proton Technologies AG
 * 
 * This file is part of ProtonVPN.
 * 
 * ProtonVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ProtonVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ProtonVPN.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.protonvpn.android.ui.login

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.protonvpn.android.R
import com.protonvpn.android.components.BaseActivityV2
import com.protonvpn.android.databinding.ActivityLoginBinding
import com.protonvpn.android.ui.home.HomeActivity
import com.protonvpn.android.ui.onboarding.OnboardingActivity
import com.protonvpn.android.ui.onboarding.OnboardingPreferences
import com.protonvpn.android.ui.onboarding.WelcomeDialog
import com.protonvpn.android.utils.AndroidUtils.launchActivity
import com.protonvpn.android.utils.Constants.SIGNUP_URL
import com.protonvpn.android.utils.Constants.URL_SUPPORT_ASSIGN_VPN_CONNECTION
import com.protonvpn.android.utils.DeepLinkActivity
import com.protonvpn.android.utils.ViewUtils.hideKeyboard
import com.protonvpn.android.utils.ViewUtils.viewBinding
import com.protonvpn.android.utils.openUrl
import com.protonvpn.android.utils.toSafeUtf8ByteArray
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.proton.core.util.kotlin.exhaustive
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener

const val LOGO_VERTICAL_BIAS = 0.5f
const val LOGO_VERTICAL_BIAS_WITH_KEYBOARD = 0.1f

@AndroidEntryPoint
class LoginActivity : BaseActivityV2(), KeyboardVisibilityEventListener, Observer<LoginState> {

    @VisibleForTesting
    val viewModel: LoginViewModel by viewModels()
    private val binding by viewBinding(ActivityLoginBinding::inflate)

    @VisibleForTesting
    fun getLoadingContainer() = binding.loadingContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!OnboardingPreferences.wasOnboardingShown()) {
            startActivity(
                Intent(this, OnboardingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                }
            )
            finish()
        }

        // Prevent screen capture etc. to record user password
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        if (viewModel.userData.isLoggedIn) {
            launchActivity<HomeActivity>()
            finish()
            return
        }
        initInputFields()
        checkIfOpenedFromWeb()
        initClickListeners()
        KeyboardVisibilityEvent.setEventListener(this, this)
        viewModel.loginState.observe(this@LoginActivity, this@LoginActivity)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initClickListeners() = with(binding) {
        buttonLogin.setOnClickListener { attemptLogin() }
        buttonCreateAccount.setOnClickListener { openUrl(SIGNUP_URL) }
        buttonNeedHelp.setOnClickListener {
            startActivity(Intent(this@LoginActivity, LoginHelpActivity::class.java))
        }
        buttonAssignVpnConnections.setOnClickListener { openUrl(URL_SUPPORT_ASSIGN_VPN_CONNECTION) }
        buttonReturnToLogin.setOnClickListener { viewModel.onBackToLogin() }
        layoutConnectionAllocationHelp.setOnClickListener { /* prevents click through */ }
    }

    private fun initInputFields() {
        with(binding) {
            switchStartWithDevice.setOnCheckedChangeListener { _, isChecked ->
                viewModel.userData.connectOnBoot = isChecked
            }
            switchStartWithDevice.visibility =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) GONE else VISIBLE
            switchStartWithDevice.isChecked = viewModel.userData.connectOnBoot
            inputPassword.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin()
                    return@OnEditorActionListener true
                }
                false
            })

            inputEmail.text = viewModel.userData.user
        }
    }

    private fun checkIfOpenedFromWeb() {
        if (intent.getBooleanExtra(DeepLinkActivity.FROM_DEEPLINK, false)) {
            binding.inputEmail.text = intent.getStringExtra(DeepLinkActivity.USER_NAME)
            WelcomeDialog.showDialog(supportFragmentManager, WelcomeDialog.DialogType.WELCOME)
        }
    }

    override fun onVisibilityChanged(isOpen: Boolean) {
        val visibility = if (isOpen) GONE else VISIBLE
        with(binding) {
            buttonCreateAccount.visibility = visibility
            buttonNeedHelp.visibility = visibility
            layoutCredentials.gravity = if (isOpen) Gravity.TOP else Gravity.CENTER_VERTICAL

            val params = protonLogo.layoutParams as ConstraintLayout.LayoutParams
            params.verticalBias = if (isOpen) LOGO_VERTICAL_BIAS_WITH_KEYBOARD else LOGO_VERTICAL_BIAS
            protonLogo.layoutParams = params
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (currentFocus is EditText) {
                val emailRect = Rect()
                val passRect = Rect()
                binding.inputEmail.getGlobalVisibleRect(emailRect)
                binding.inputPassword.getGlobalVisibleRect(passRect)
                if (!emailRect.contains(event.rawX.toInt(), event.rawY.toInt()) &&
                        !passRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    hideKeyboard()
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun attemptLogin() = with(binding) {
        inputEmail.clearInputError()
        inputPassword.clearInputError()

        val email = inputEmail.text.toString()
        var cancel = false
        var focusView: View? = null

        if (TextUtils.isEmpty(email)) {
            inputEmail.setInputError(getString(R.string.error_field_required))
            focusView = inputEmail
            cancel = true
        }

        if (TextUtils.isEmpty(inputPassword.text)) {
            inputPassword.setInputError(getString(R.string.error_field_required))
            focusView = inputPassword
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            hideKeyboard()
            viewModel.userData.user = email

            login()
            loadingContainer.setRetryListener(::login)
        }
    }

    private var loginJob: Job? = null

    override fun onVpnPrepareFailed() {
        loginJob?.cancel()
        viewModel.onVpnPrepareFailed()
    }

    private fun login() = with(binding) {
        if (loginJob?.isActive != true) {
            loginJob = lifecycleScope.launch {
                viewModel.login(this@LoginActivity, inputEmail.text.toString(),
                    inputPassword.text!!.toSafeUtf8ByteArray())
            }
        }
    }

    override fun onBackPressed() {
        val handled = viewModel.onBackPressed()
        if (!handled) {
            super.onBackPressed()
        }
    }

    override fun onChanged(loginState: LoginState) {
        with(binding) {
            binding.layoutConnectionAllocationHelp.isVisible =
                loginState == LoginState.ConnectionAllocationPrompt
            when (loginState) {
                is LoginState.EnterCredentials -> {
                    loadingContainer.switchToEmpty()
                }
                is LoginState.Success -> {
                    launchActivity<HomeActivity>()
                    inputPassword.clearTextAndOverwriteMemory()
                    finish()
                }
                is LoginState.InProgress -> {
                    loadingContainer.switchToLoading()
                }
                is LoginState.GuestHoleActivated -> {
                    loadingContainer.switchToLoading(getString(R.string.guestHoleActivated))
                }
                is LoginState.Error -> {
                    if (loginState.retryRequest.not())
                        loadingContainer.setRetryListener(loadingContainer::switchToEmpty)
                    loadingContainer.switchToRetry(loginState.error)
                }
                is LoginState.UnsupportedAuth -> {
                    loadingContainer.switchToEmpty()
                    snackbarHelper.errorSnack(R.string.toastLoginAuthVersionError)
                }
                is LoginState.ConnectionAllocationPrompt -> {
                    loadingContainer.switchToEmpty()
                }
                is LoginState.RetryLogin -> attemptLogin()
            }.exhaustive
        }
    }
}
