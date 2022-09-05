package org.zotero.android.root.usecase

import org.zotero.android.root.repository.AuthRepository
import javax.inject.Inject

class UserIsLoggedInUseCase @Inject constructor(
    private val authRepo: AuthRepository,
) {
    fun execute(): Boolean = authRepo.isUserLoggedIn()
}
