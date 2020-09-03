package eu.vojtechh.speedometer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import eu.vojtechh.speedometer.databinding.ActivityMainBinding
import eu.vojtechh.speedometer.other.Constants.ACTION_SHOW_TRACKING_UI
import eu.vojtechh.speedometer.utils.SharedPref

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var launchedFromService = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        restoreTrackingFragmentIfNeeded(intent)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        SharedPref.init(applicationContext)

        setSupportActionBar(binding.toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.my_nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.setGraph(
            R.navigation.nav_graph,
            bundleOf(ACTION_SHOW_TRACKING_UI to launchedFromService)
        )

        navController.restoreState(savedInstanceState)

        binding.bottomNavigationView.setupWithNavController(navController)

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        restoreTrackingFragmentIfNeeded(intent)
    }

    private fun restoreTrackingFragmentIfNeeded(intent: Intent?) {
        if (intent?.action == ACTION_SHOW_TRACKING_UI) {
            launchedFromService = true
        }
    }
}