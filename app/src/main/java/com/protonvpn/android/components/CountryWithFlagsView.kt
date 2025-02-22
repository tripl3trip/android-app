/*
 * Copyright (c) 2021. Proton Technologies AG
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

package com.protonvpn.android.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.protonvpn.android.R
import com.protonvpn.android.databinding.ConnectionFlagsViewBinding
import com.protonvpn.android.models.vpn.Server
import com.protonvpn.android.utils.AndroidUtils.getFloatRes
import com.protonvpn.android.utils.CountryTools

class CountryWithFlagsView : LinearLayout {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initAttrs(attrs)
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initAttrs(attrs)
    }

    val binding = ConnectionFlagsViewBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = HORIZONTAL
    }

    private fun initAttrs(attrs: AttributeSet?) {
        context.withStyledAttributes(attrs, R.styleable.CountryWithFlagsView) {
            TextViewCompat.setTextAppearance(
                binding.textCountry,
                getResourceId(R.styleable.CountryWithFlagsView_android_textAppearance, -1)
            )
        }
    }

    fun setCountry(markable: Markable) {
        with(binding) {
            val isSecureCore = markable.markerEntryCountryCode != null
            imageSCArrow.isVisible = isSecureCore
            imageEntryCountry.isVisible = isSecureCore
            if (isSecureCore) {
                imageEntryCountry.setFlag(markable.markerEntryCountryCode)
            }

            imageExitCountry.setFlag(markable.markerCountryCode)
            textCountry.text = CountryTools.getFullName(markable.markerCountryCode)
        }
    }

    fun setCountry(server: Server) {
        with(binding) {
            val isSecureCore = server.isSecureCoreServer
            imageSCArrow.isVisible = isSecureCore
            imageEntryCountry.isVisible = isSecureCore
            if (isSecureCore) {
                imageEntryCountry.setFlag(server.entryCountry)
            }
            imageExitCountry.setFlag(server.flag)
            textCountry.text = CountryTools.getFullName(server.flag)
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        with(binding) {
            val flagAlpha = if (enabled) 1f else resources.getFloatRes(R.dimen.inactive_flag_alpha)
            textCountry.isEnabled = enabled
            imageEntryCountry.alpha = flagAlpha
            imageExitCountry.alpha = flagAlpha
        }
    }

    private fun ImageView.setFlag(flag: String) =
        setImageResource(CountryTools.getFlagResource(context, flag))
}
