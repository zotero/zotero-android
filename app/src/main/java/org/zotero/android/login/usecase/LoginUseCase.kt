package org.zotero.android.login.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.repositories.AccountRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val dispatcher: CoroutineDispatcher,
    private val accountRepository: AccountRepository,
) {
    suspend fun execute(
        username: String,
        password: String,
    ): CustomResult<Unit> = withContext(dispatcher) {
        val networkResult = accountRepository.login(username = username, password = password)
        if (networkResult !is CustomResult.GeneralSuccess) {
            return@withContext networkResult as CustomResult.GeneralError
        }
        CustomResult.GeneralSuccess(Unit)
    }

}
