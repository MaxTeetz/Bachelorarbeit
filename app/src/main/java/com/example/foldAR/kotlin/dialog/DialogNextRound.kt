package com.example.foldAR.kotlin.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.foldAR.kotlin.helloar.R
import com.example.foldAR.kotlin.helloar.databinding.DialogNextRoundBinding
import com.example.foldAR.kotlin.mainActivity.MainActivity
import com.example.foldAR.kotlin.mainActivity.MainActivityViewModel

class DialogNextRound : DialogFragment() {

    companion object {
        fun newInstance() = DialogNextRound().apply {
            arguments = Bundle().apply {}
        }
    }

    private lateinit var activity: MainActivity

    private var _binding: DialogNextRoundBinding? = null
    private val binding get() = _binding!!

    private val viewModelMainActivity: MainActivityViewModel by activityViewModels()

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
            isCancelable = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogNextRoundBinding.inflate(inflater, container, false)
        activity = requireActivity() as MainActivity

        return binding.root
    }

    @SuppressLint("StringFormatMatches")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.currentRound.text =
            getString(R.string.naechste_runde_aus_20, viewModelMainActivity.targetIndex.value!!)

        binding.nextRound.setOnClickListener {
            checkDismiss()
        }
    }

    private fun checkDismiss() {
        val state = viewModelMainActivity.checkCorrectUserPosition()
        if (state.isBlank())
            dismissDialog()
        else
            Toast.makeText(requireContext(), state, Toast.LENGTH_LONG).show()
    }

    private fun dismissDialog() {
        if (viewModelMainActivity.renderer.wrappedAnchors.isNotEmpty()) {
            viewModelMainActivity.placeTargetOnNewPosition()
            viewModelMainActivity.placeObjectInFocus()
            viewModelMainActivity.renderer.resetReached()
            viewModelMainActivity.updateTestCaseStartTime()
        }
        viewModelMainActivity.setIsAlertDialogOpen(false)
        this.dismiss()
    }

}