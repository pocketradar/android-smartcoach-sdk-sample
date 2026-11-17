package com.pocketradar.smartcoach.sample

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pocketradar.smartcoach.SmartCoachError
import com.pocketradar.smartcoach.SmartCoachErrorCode

/** The error's message and code, plus what to do about it for the errors an integrator can fix. */
@Composable
fun errorText(error: SmartCoachError): String {
    val text = stringResource(R.string.error_message, error.message.orEmpty(), error.code.rawValue)
    val hint = hint(error.code) ?: return text
    return text + "\n\n" + stringResource(hint)
}

@StringRes
private fun hint(code: SmartCoachErrorCode): Int? = when (code) {
    SmartCoachErrorCode.invalidEntitlements -> R.string.hint_invalid_entitlements
    SmartCoachErrorCode.featureNotAvailable -> R.string.hint_feature_not_available
    SmartCoachErrorCode.permissionDenied -> R.string.hint_permission_denied
    else -> null
}
