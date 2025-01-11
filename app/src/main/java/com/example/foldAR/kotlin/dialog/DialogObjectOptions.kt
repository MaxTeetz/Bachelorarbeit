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
import com.example.foldAR.data.AppDatabase
import com.example.foldAR.data.DatabaseApplication
import com.example.foldAR.data.entities.Users
import com.example.foldAR.kotlin.helloar.databinding.DialogObjectOptionsBinding
import com.example.foldAR.kotlin.mainActivity.MainActivityViewModel
import com.example.foldAR.kotlin.renderer.WrappedAnchor
import kotlinx.coroutines.launch

class DialogObjectOptions : DialogFragment() {

    companion object {
        fun newInstance() = DialogObjectOptions().apply {
            arguments = Bundle().apply {}
        }
    }

    private val database: AppDatabase by lazy {
        (requireActivity().application as DatabaseApplication).database
    }

    private var _binding: DialogObjectOptionsBinding? = null
    private val binding get() = _binding!!

    private val viewModelMainActivity: MainActivityViewModel by activityViewModels()

    private var displayWidth: Int? = null

    private lateinit var name: String

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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpListener()

    }

    private fun setUpListener() {
        binding.nextObject.setOnClickListener {
            checkCorrect()
        }
    }

    private fun checkCorrect() {
        this.name = binding.name.text.toString()
        val message = getErrorMessage(name, viewModelMainActivity.renderer.wrappedAnchors)

        if (message != null)
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        else
            startTest(name)
    }

    private fun getErrorMessage(name: String, wrappedAnchors: MutableList<WrappedAnchor>): String? {
        return when {
            wrappedAnchors.isEmpty() -> "Bitte erst Objekt platzieren"

            name.isEmpty() -> "Bitte erst Namen eintragen"

            else -> null
        }
    }

    private fun startTest(name: String) {

        lifecycleScope.launch {
            database.usersDao().insertUser(Users(Username = name))
        }

        viewModelMainActivity.createTarget()
        viewModelMainActivity.setClickable(false)
        viewModelMainActivity.placeTargetOnNewPosition()
        viewModelMainActivity.placeObjectInFocus()

        this.dismiss()
    }

    override fun onResume() {
        super.onResume()
        displayWidth = dialog?.window?.attributes?.width
    }
}