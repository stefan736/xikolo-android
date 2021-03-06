package de.xikolo.network.jobs

import android.util.Log
import de.xikolo.config.Config
import de.xikolo.managers.UserManager
import de.xikolo.models.Enrollment
import de.xikolo.network.ApiService
import de.xikolo.network.jobs.base.NetworkJob
import de.xikolo.network.jobs.base.NetworkStateLiveData
import de.xikolo.network.sync.Sync
import ru.gildor.coroutines.retrofit.awaitResponse

class GetCourseJob(private val courseId: String, networkState: NetworkStateLiveData, userRequest: Boolean) : NetworkJob(networkState, userRequest) {

    companion object {
        val TAG: String = GetCourseJob::class.java.simpleName
    }

    override suspend fun onRun() {
        val response = if (UserManager.isAuthorized) {
            ApiService.instance.getCourseWithEnrollment(courseId).awaitResponse()
        } else {
            ApiService.instance.getCourse(courseId).awaitResponse()
        }

        if (response.isSuccessful && response.body() != null) {
            if (Config.DEBUG) Log.i(TAG, "Course received")

            Sync.Data.with(response.body()!!)
                .saveOnly()
                .run()
            Sync.Included.with<Enrollment>(response.body()!!)
                .addFilter("courseId", courseId)
                .run()

            success()
        } else {
            if (Config.DEBUG) Log.e(TAG, "Error while fetching course")
            error()
        }
    }

}
