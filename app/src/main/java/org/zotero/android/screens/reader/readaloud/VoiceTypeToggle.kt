package org.zotero.android.screens.reader.readaloud

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.reader.readaloud.voiceselection.ReadAloudVoiceType
import org.zotero.android.uicomponents.Strings

private val onboardingVoiceTypes = listOf(
    ReadAloudVoiceType.standard,
    ReadAloudVoiceType.premium,
    ReadAloudVoiceType.local,
)

@Composable
internal fun VoiceTypeToggle(
    selected: ReadAloudVoiceType,
    onSelected: (ReadAloudVoiceType) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        onboardingVoiceTypes.forEachIndexed { index, type ->
            SegmentedButton(
                selected = type == selected,
                onClick = { onSelected(type) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = onboardingVoiceTypes.size),
            ) {
                Text(text = stringResource(type.titleRes()))
            }
        }
    }
}

internal fun ReadAloudVoiceType.titleRes(): Int = when (this) {
    ReadAloudVoiceType.premium -> Strings.speech_onboarding_tier_premium
    ReadAloudVoiceType.standard -> Strings.speech_onboarding_tier_standard
    ReadAloudVoiceType.local -> Strings.speech_onboarding_tier_local
}

internal fun ReadAloudVoiceType.descriptionRes(): List<Int> = when (this) {
    ReadAloudVoiceType.premium -> listOf(
        Strings.speech_onboarding_description_premium_1,
        Strings.speech_onboarding_description_premium_2,
        Strings.speech_onboarding_description_premium_3,
        Strings.speech_onboarding_description_premium_4,
        Strings.speech_onboarding_description_premium_5,
        Strings.speech_onboarding_description_premium_6,
    )

    ReadAloudVoiceType.standard -> listOf(
        Strings.speech_onboarding_description_standard_1,
        Strings.speech_onboarding_description_standard_2,
        Strings.speech_onboarding_description_standard_3,
        Strings.speech_onboarding_description_standard_4,
        Strings.speech_onboarding_description_standard_5,
        Strings.speech_onboarding_description_standard_6,
    )

    ReadAloudVoiceType.local -> listOf(
        Strings.speech_onboarding_description_local_1,
        Strings.speech_onboarding_description_local_2,
        Strings.speech_onboarding_description_local_3,
    )
}