package com.example.foldAR.kotlin.dialog

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.foldAR.kotlin.helloar.databinding.DialogObjectOptionsBinding
import com.example.foldAR.kotlin.mainActivity.MainActivity
import com.example.foldAR.kotlin.mainActivity.MainActivityViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DialogObjectOptions : DialogFragment() {

    companion object {
        fun newInstance() = DialogObjectOptions().apply {
            arguments = Bundle().apply {}
        }
    }

    private lateinit var activity: MainActivity

    private var _binding: DialogObjectOptionsBinding? = null
    private val binding get() = _binding!!

    private val viewModelMainActivity: MainActivityViewModel by activityViewModels()

    private var displayWidth: Int? = null

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.854).toInt()

        dialog!!.window?.apply {
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)

            val params = attributes
            params.gravity = Gravity.TOP
            params.y = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                125f,
                resources.displayMetrics
            ).toInt()
            attributes = params
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogObjectOptionsBinding.inflate(inflater, container, false)
        activity = requireActivity() as MainActivity

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpListener()

    }

    private fun setUpListener() {
        binding.startTesting.setOnClickListener {
            checkUser()
        }
    }

    private fun checkUser() {
        val name = binding.name.text.toString()
        if (viewModelMainActivity.renderer.wrappedAnchors.isEmpty()) {
            makeToast("Erst Objekt platzieren!")
        } else {
            if (viewModelMainActivity.currentUser.value != null) {
                showAlert()
            } else {

                lifecycleScope.launch(Dispatchers.Main) {

                    if (name.isEmpty())
                        makeToast("Erst Namen eingeben")
                    else { //TODO if user is created after another one finishes a dialog appears with endTarget as Text
                        viewModelMainActivity.currentTestCase.observe(viewLifecycleOwner) { testcase ->
                            if (testcase != null) {
                                showAlert()
                                viewModelMainActivity.currentTestCase.removeObservers(
                                    viewLifecycleOwner
                                )
                            }
                        }
                        viewModelMainActivity.insertUser(name)
                    }
                }
            }
        }
    }


    private fun makeToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun showAlert() {

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(android.R.string.dialog_alert_title))
            .setMessage("Die nächste Runde ist Scenario ${viewModelMainActivity.currentScenario.value!!.ScenarioCase}")
            .setCancelable(false)
            .setPositiveButton("Nächste Runde") { dialogInterface, _ ->
                dialogInterface.dismiss()
                viewModelMainActivity.updateTestCaseStartTime()
                startUI()
            }
            .show()
    }


    private fun startUI() {

        viewModelMainActivity.setDatabaseObjectsSet(false)
        viewModelMainActivity.createTarget()
        viewModelMainActivity.placeTargetOnNewPosition()
        viewModelMainActivity.placeObjectInFocus()

        this.dismiss()

    }

    override fun onDestroyView() {
        viewModelMainActivity.currentTestCase.removeObservers(viewLifecycleOwner)
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        displayWidth = dialog?.window?.attributes?.width
    }
}