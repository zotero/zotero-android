package org.zotero.android.login.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.zotero.android.api.network.NetworkResultWrapper
import org.zotero.android.api.repositories.AccountRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val dispatcher: CoroutineDispatcher,
    private val accountRepository: AccountRepository,
) {
    suspend fun execute(
        username: String,
        password: String,
    ): NetworkResultWrapper<Unit> = withContext(dispatcher) {
        val networkResult = accountRepository.login(username = username, password = password)
        if (networkResult !is NetworkResultWrapper.Success) {
            return@withContext networkResult as NetworkResultWrapper.NetworkError
        }
        NetworkResultWrapper.Success(Unit)
    }

}
