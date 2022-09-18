package mega.privacy.android.app.data.mapper

import mega.privacy.android.domain.entity.contacts.ContactData

/**
 * Mapper to convert data to [ContactData]
 */
typealias ContactDataMapper = (
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards String,
) -> ContactData

internal fun toContactData(
    fullName: String?,
    alias: String?,
    avatarUri: String?,
    defaultAvatarContent: String,
): ContactData = ContactData(
    fullName = fullName,
    alias = alias,
    avatarUri = avatarUri,
    defaultAvatarContent = defaultAvatarContent
)