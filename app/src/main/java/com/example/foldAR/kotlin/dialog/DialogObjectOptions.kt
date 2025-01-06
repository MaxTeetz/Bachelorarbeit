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
import com.example.foldAR.kotlin.helloar.databinding.DialogObjectOptionsBinding
import com.example.foldAR.kotlin.mainActivity.MainActivityViewModel

class DialogObjectOptions : DialogFragment() {

    companion object {
        fun newInstance() = DialogObjectOptions().apply {
            arguments = Bundle().apply {}
        }
    }

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

    private fun startTest() {
        if (viewModelMainActivity.renderer.wrappedAnchors.isEmpty())
            Toast.makeText(requireContext(), "Please place Object first", Toast.LENGTH_LONG).show()
        else {
            viewModelMainActivity.createTarget()
            viewModelMainActivity.setIndex()
            viewModelMainActivity.setClickable(false)
            this.dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogObjectOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpListener()
        setText()

    }

    private fun setUpListener() {
        binding.nextObject.setOnClickListener {
            startTest()
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun setText() {
        binding.progress.text = getString(R.string.numOutOf20, viewModelMainActivity.getIndex())
    }

    override fun onResume() {
        super.onResume()
        displayWidth = dialog?.window?.attributes?.width
    }
}