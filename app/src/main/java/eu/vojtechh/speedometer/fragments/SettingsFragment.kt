package eu.vojtechh.speedometer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import eu.vojtechh.speedometer.R
import eu.vojtechh.speedometer.databinding.FragmentSettingsBinding
import eu.vojtechh.speedometer.utils.SharedPref

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null

    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.wheelDiamInput.setText(SharedPref.read(SharedPref.WHEEL_DIAMETER, 62.2f).toString())


        binding.saveConfButton.setOnClickListener {
            val newWheelDiam = binding.wheelDiamInput.text;
            if (newWheelDiam.isNotEmpty()) {
                SharedPref.write(SharedPref.WHEEL_DIAMETER, newWheelDiam.toString().toFloat())
                Snackbar.make(
                    binding.saveConfButton,
                    R.string.configuration_saved,
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(R.id.bottom_navigation_view).show()
            } else {
                Snackbar.make(
                    binding.saveConfButton,
                    R.string.empty_wheel_diam,
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(R.id.bottom_navigation_view).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}