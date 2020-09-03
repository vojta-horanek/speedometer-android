package eu.vojtechh.speedometer.fragments

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.Snackbar
import eu.vojtechh.speedometer.R
import eu.vojtechh.speedometer.databinding.FragmentTrackingBinding
import eu.vojtechh.speedometer.models.DeviceInfo
import eu.vojtechh.speedometer.other.Constants.ACTION_PAUSE_SERVICE
import eu.vojtechh.speedometer.other.Constants.ACTION_SHOW_TRACKING_UI
import eu.vojtechh.speedometer.other.Constants.ACTION_START_OR_RESUME_SERVICE
import eu.vojtechh.speedometer.other.Constants.ACTION_STOP_SERVICE
import eu.vojtechh.speedometer.other.Constants.DEVICE_SSID
import eu.vojtechh.speedometer.other.Constants.INTENT_ACTION
import eu.vojtechh.speedometer.other.Constants.KEY_CONNECTED
import eu.vojtechh.speedometer.other.Constants.KEY_ERROR
import eu.vojtechh.speedometer.other.Constants.KEY_MSG_TYPE
import eu.vojtechh.speedometer.other.Constants.KEY_READY
import eu.vojtechh.speedometer.other.Constants.KEY_STOPPED
import eu.vojtechh.speedometer.other.Constants.MSG_TYPE_ERROR
import eu.vojtechh.speedometer.other.Constants.MSG_TYPE_STATUS
import eu.vojtechh.speedometer.other.Constants.MSG_TYPE_WIFI_CALLBACK
import eu.vojtechh.speedometer.other.Constants.REQUEST_CODE_LOCATION_PERMISSION
import eu.vojtechh.speedometer.services.TrackingService
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.util.*
import java.util.concurrent.TimeUnit


class TrackingFragment : Fragment(R.layout.fragment_tracking), EasyPermissions.PermissionCallbacks {


    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!
    private lateinit var connectivityManager: ConnectivityManager

    private var isTracking = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestPermissions()
        connectivityManager =
            requireContext().getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            (intentReceived),
            IntentFilter(INTENT_ACTION)
        )
        initializeUi()
    }

    private fun hasLocationPermissions() =
        EasyPermissions.hasPermissions(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )


    private fun requestPermissions() {
        if (hasLocationPermissions()) return

        EasyPermissions.requestPermissions(
            this,
            "You need to accept location permissions to use this app.",
            REQUEST_CODE_LOCATION_PERMISSION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermissions()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {}

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun showSnackbar(text: String) =
        Snackbar.make(
            binding.root,
            text,
            Snackbar.LENGTH_SHORT
        ).setAnchorView(R.id.bottom_navigation_view).show()


    private val intentReceived: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.getStringExtra(KEY_MSG_TYPE)?.let {
                when (it) {
                    MSG_TYPE_ERROR -> {
                        showSnackbar(intent.getStringExtra(KEY_ERROR)!!)
                    }
                    MSG_TYPE_STATUS -> {
                        if (intent.getBooleanExtra(KEY_READY, false)) {
                            subscribeToObservers()
                            showTrackingUi(true)
                        } else if (intent.getBooleanExtra(KEY_STOPPED, false)) {
                            showTrackingUi(false)
                        }
                    }
                    MSG_TYPE_WIFI_CALLBACK -> {
                        if (intent.getBooleanExtra(KEY_CONNECTED, false)) {
                            showDeviceConnectedUi(getConnectedState())
                        }
                    }
                }
            }

        }
    }

    private val networkCallback: ConnectivityManager.NetworkCallback =
        object : ConnectivityManager.NetworkCallback() {

            private fun broadcast() {
                LocalBroadcastManager.getInstance(requireContext())
                    .sendBroadcast(Intent(INTENT_ACTION).apply {
                        putExtra(KEY_MSG_TYPE, MSG_TYPE_WIFI_CALLBACK)
                        putExtra(KEY_CONNECTED, true)
                    })
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                broadcast()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                broadcast()
            }
        }

    private fun initializeUi() {
        //TODO Show tracking ui if TrackingService is running -> Test
        arguments?.getBoolean(ACTION_SHOW_TRACKING_UI)?.let {
            showTrackingUi(it)
        }

        showDeviceConnectedUi(getConnectedState())

        binding.stopButton.setOnClickListener { stopTracking() }

        binding.pauseButton.setOnClickListener { pauseTracking() }
    }

    private fun getConnectedState(): Boolean {
        if (isInternetAvailable()) {
            val mWifiManager: WifiManager =
                (requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager)
            val info: WifiInfo = mWifiManager.connectionInfo
            return info.ssid.contains(DEVICE_SSID)
        }
        return false
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            else -> false
        }

    }

    private fun showDeviceConnectedUi(show: Boolean) {
        val primaryColor: Int
        val iconReady: Int
        if (show) {
            primaryColor = ContextCompat.getColor(requireContext(), R.color.greenReady)
            iconReady = R.drawable.ic_connected

            binding.textReadyStatus.setText(R.string.connected)
            binding.startButton.visibility = View.VISIBLE
            binding.iconTap.visibility = View.GONE
            binding.textTap.visibility = View.GONE
            binding.startButton.setOnClickListener {
                startTracking()
            }
        } else {
            primaryColor = ContextCompat.getColor(requireContext(), R.color.redNotReady)
            iconReady = R.drawable.ic_disconnected

            binding.textReadyStatus.setText(R.string.disconnected)
            binding.startButton.visibility = View.GONE
            binding.iconTap.visibility = View.VISIBLE
            binding.textTap.visibility = View.VISIBLE
        }

        binding.textReadyStatus.setTextColor(primaryColor)
        ImageViewCompat.setImageTintList(
            binding.iconReadyStatus, ColorStateList.valueOf(primaryColor)
        )
        binding.iconReadyStatus.setImageDrawable(
            ContextCompat.getDrawable(requireContext(), iconReady)
        )
    }

    private fun subscribeToObservers() {
        TrackingService.apply {
            isTracking.observe(viewLifecycleOwner, { updateTracking(it) })
            deviceData.observe(viewLifecycleOwner, { updateInfoScreen(it) })
            durationMillis.observe(viewLifecycleOwner, { updateDisplayTime(it) })
        }

    }

    private fun getFormattedStopWatchTime(ms: Long, includeMillis: Boolean = false): String {
        var milliseconds = ms
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        milliseconds -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)
        if (!includeMillis) {
            return "${if (hours < 10) "0" else ""}$hours:" +
                    "${if (minutes < 10) "0" else ""}$minutes:" +
                    "${if (seconds < 10) "0" else ""}$seconds"
        }
        milliseconds -= TimeUnit.SECONDS.toMillis(seconds)
        milliseconds /= 10
        return "${if (hours < 10) "0" else ""}$hours:" +
                "${if (minutes < 10) "0" else ""}$minutes:" +
                "${if (seconds < 10) "0" else ""}$seconds:" +
                "${if (milliseconds < 10) "0" else ""}$milliseconds"
    }

    private fun updateDisplayTime(durationMillis: Long) {
        binding.infoDuration.text = getFormattedStopWatchTime(durationMillis)
    }

    private fun updateTracking(tracking: Boolean) {
        isTracking = tracking
        Snackbar.make(
            binding.root,
            if (isTracking) R.string.tracking_started else R.string.tracking_paused,
            Snackbar.LENGTH_SHORT
        ).setAnchorView(R.id.bottom_navigation_view).show()

        if (isTracking) {
            binding.pauseButton.setIconResource(R.drawable.ic_pause)
            binding.pauseButton.setText(R.string.pause)
        } else {
            binding.pauseButton.setIconResource(R.drawable.ic_start)
            binding.pauseButton.setText(R.string.resume)
        }
    }

    private fun updateInfoScreen(update: DeviceInfo) {
        if (_binding != null && isTracking) {
            binding.infoSpeed.text =
                String.format(
                    Locale.US,
                    "%.1f",
                    update.speed
                )
            binding.infoDistance.text =
                String.format(
                    Locale.US,
                    "%.2f",
                    update.distance
                )
        }
    }

    private fun showTrackingUi(show: Boolean) {
        if (show) {
            binding.trackingStartLayout.visibility = View.GONE
            binding.trackingMeasureLayout.visibility = View.VISIBLE

            Snackbar.make(
                binding.startButton,
                R.string.tracking_started,
                Snackbar.LENGTH_SHORT
            ).setAnchorView(R.id.bottom_navigation_view).show()

        } else {
            binding.trackingMeasureLayout.visibility = View.GONE
            binding.trackingStartLayout.visibility = View.VISIBLE

            Snackbar.make(
                binding.stopButton,
                R.string.tracking_stopped,
                Snackbar.LENGTH_SHORT
            ).setAnchorView(R.id.bottom_navigation_view).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(intentReceived)
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private fun startTracking() =
        sendCommandToService(ACTION_START_OR_RESUME_SERVICE)

    private fun stopTracking() =
        sendCommandToService(ACTION_STOP_SERVICE)

    private fun pauseTracking() {
        if (isTracking) {
            sendCommandToService(ACTION_PAUSE_SERVICE)
        } else {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }


}