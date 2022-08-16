package org.zotero.android.sample.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.zotero.android.architecture.Result
import org.zotero.android.sample.model.SamplePojo
import javax.inject.Inject

class SampleUseCase @Inject constructor(
    private val dispatcher: CoroutineDispatcher
) {
    suspend fun execute(
    ): Result<SamplePojo> = withContext(dispatcher) {
        Result.Success(SamplePojo(samplePayload = "payload string"))
    }
}
