package de.xikolo.controllers.main

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatActivity
import de.xikolo.controllers.dialogs.*
import de.xikolo.jobs.CheckHealthJob
import de.xikolo.jobs.base.RequestJobCallback
import de.xikolo.storages.ApplicationPreferences
import de.xikolo.utils.DateUtil
import java.util.*
import java.util.concurrent.TimeUnit

class SplashActivity : AppCompatActivity() {

    companion object {
        val TAG: String = SplashActivity::class.java.simpleName
    }

    private val healthCheckCallback: RequestJobCallback
        get() = object : RequestJobCallback() {
            override fun onSuccess() {
                startApp()
            }

            override fun onError(code: RequestJobCallback.ErrorCode) {
                when (code) {
                    ErrorCode.NO_NETWORK ->             startApp()
                    ErrorCode.API_VERSION_EXPIRED ->    showApiVersionExpiredDialog()
                    ErrorCode.MAINTENANCE ->            showServerMaintenanceDialog()
                    else ->                             showServerErrorDialog()
                }
            }

            override fun onDeprecated(deprecationDate: Date) {
                val now = Date()
                val distance = deprecationDate.time - now.time
                val days = TimeUnit.DAYS.convert(distance, TimeUnit.MILLISECONDS)

                if (days <= 14) {
                    showApiVersionDeprecatedDialog(deprecationDate)
                } else {
                    startApp()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            when {
                DateUtil.isPast("2018-07-15T00:00:00.000") && !ApplicationPreferences().firstAndroid4DeprecationWarningShown -> {
                    val dialog = Android4DeprecatedDialog()
                    dialog.listener = object : Android4DeprecatedDialog.Listener {
                        override fun onConfirmed() {
                            CheckHealthJob(healthCheckCallback).run()
                        }
                    }
                    showDialog(dialog, Android4DeprecatedDialog.TAG)
                    ApplicationPreferences().firstAndroid4DeprecationWarningShown = true
                }
                DateUtil.isPast("2018-08-15T00:00:00.000") && !ApplicationPreferences().secondAndroid4DeprecationWarningShown -> {
                    val dialog = Android4DeprecatedDialog()
                    dialog.listener = object : Android4DeprecatedDialog.Listener {
                        override fun onConfirmed() {
                            CheckHealthJob(healthCheckCallback).run()
                        }
                    }
                    showDialog(dialog, Android4DeprecatedDialog.TAG)
                    ApplicationPreferences().secondAndroid4DeprecationWarningShown = true
                }
                DateUtil.isPast("2018-08-31T00:00:00.000") -> {
                    val dialog = Android4UnsupportedDialog()
                    dialog.listener = object : Android4UnsupportedDialog.Listener {
                        override fun onConfirmed() {
                            closeApp()
                        }
                    }
                    showDialog(dialog, Android4DeprecatedDialog.TAG)
                }
                else -> {
                    CheckHealthJob(healthCheckCallback).run()
                }
            }
        } else {
            CheckHealthJob(healthCheckCallback).run()
        }
    }

    private fun showApiVersionExpiredDialog() {
        val dialog = ApiVersionExpiredDialog()
        dialog.setDialogListener(object : ApiVersionExpiredDialog.Listener {
            override fun onOpenPlayStoreClicked() {
                openPlayStore()
            }

            override fun onDismissed() {
                closeApp()
            }
        })
        showDialog(dialog, ApiVersionExpiredDialog.TAG)
    }

    private fun showApiVersionDeprecatedDialog(deprecationDate: Date) {
        val dialog = ApiVersionDeprecatedDialogAutoBundle.builder(deprecationDate).build()
        dialog.setDialogListener(object : ApiVersionDeprecatedDialog.Listener {
            override fun onOpenPlayStoreClicked() {
                openPlayStore()
            }

            override fun onDismissed() {
                startApp()
            }
        })
        showDialog(dialog, ApiVersionDeprecatedDialog.TAG)
    }

    private fun showServerMaintenanceDialog() {
        val dialog = ServerMaintenanceDialog()
        dialog.setDialogListener({ this.closeApp() })
        showDialog(dialog, ServerMaintenanceDialog.TAG)
    }

    private fun showServerErrorDialog() {
        val dialog = ServerErrorDialog()
        dialog.setDialogListener({ this.closeApp() })
        showDialog(dialog, ServerErrorDialog.TAG)
    }

    private fun startApp() {
        val intent = Intent(this@SplashActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun closeApp() {
        finish()
    }

    private fun openPlayStore() {
        val appPackageName = packageName
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$appPackageName")
                )
            )
        } catch (e: android.content.ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                )
            )
        }

        finish()
    }

    // bug fix workaround
    private fun showDialog(dialogFragment: DialogFragment, tag: String) {
        val ft = supportFragmentManager.beginTransaction()
        ft.add(dialogFragment, tag)
        ft.commitAllowingStateLoss()
    }

}