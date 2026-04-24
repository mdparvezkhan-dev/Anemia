package com.example.anemia

import android.animation.ObjectAnimator
import android.app.Dialog
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.view.Window
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

class MainActivity : AppCompatActivity() {

    private lateinit var env: OrtEnvironment
    private lateinit var session: OrtSession

    private lateinit var etAge: EditText
    private lateinit var spSex: Spinner
    private lateinit var etHemoglobin: EditText
    private lateinit var etESR: EditText
    private lateinit var etWBC: EditText
    private lateinit var etNeutrophils: EditText
    private lateinit var etLymphocytes: EditText
    private lateinit var etMonocytes: EditText
    private lateinit var etEosinophils: EditText
    private lateinit var etBasophils: EditText
    private lateinit var etRBC: EditText
    private lateinit var etHCT: EditText
    private lateinit var etMCV: EditText
    private lateinit var etMCH: EditText
    private lateinit var etMCHC: EditText
    private lateinit var etRDWCV: EditText
    private lateinit var etRDWSD: EditText
    private lateinit var etPlatelet: EditText
    private lateinit var etMPV: EditText
    private lateinit var etPDW: EditText
    private lateinit var etPCT: EditText
    private lateinit var btnPredict: MaterialButton
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadModel()
        setupKeyboardHandling()

        btnPredict.setOnClickListener {
            if (validateInputs()) {
                hideKeyboard()
                predict()
            }
        }
    }

    private fun initViews() {
        scrollView = findViewById(R.id.scrollView)
        etAge = findViewById(R.id.etAge)
        spSex = findViewById(R.id.spSex)
        etHemoglobin = findViewById(R.id.etHemoglobin)
        etESR = findViewById(R.id.etESR)
        etWBC = findViewById(R.id.etWBC)
        etNeutrophils = findViewById(R.id.etNeutrophils)
        etLymphocytes = findViewById(R.id.etLymphocytes)
        etMonocytes = findViewById(R.id.etMonocytes)
        etEosinophils = findViewById(R.id.etEosinophils)
        etBasophils = findViewById(R.id.etBasophils)
        etRBC = findViewById(R.id.etRBC)
        etHCT = findViewById(R.id.etHCT)
        etMCV = findViewById(R.id.etMCV)
        etMCH = findViewById(R.id.etMCH)
        etMCHC = findViewById(R.id.etMCHC)
        etRDWCV = findViewById(R.id.etRDWCV)
        etRDWSD = findViewById(R.id.etRDWSD)
        etPlatelet = findViewById(R.id.etPlatelet)
        etMPV = findViewById(R.id.etMPV)
        etPDW = findViewById(R.id.etPDW)
        etPCT = findViewById(R.id.etPCT)
        btnPredict = findViewById(R.id.btnPredict)
    }

    private fun setupKeyboardHandling() {
        val allFields = listOf(
            etAge, etHemoglobin, etESR, etWBC, etNeutrophils,
            etLymphocytes, etMonocytes, etEosinophils, etBasophils, etRBC,
            etHCT, etMCV, etMCH, etMCHC, etRDWCV, etRDWSD, etPlatelet,
            etMPV, etPDW, etPCT
        )

        for (field in allFields) {
            field.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        // Get actual position on screen
                        val location = IntArray(2)
                        view.getLocationInWindow(location)

                        val scrollViewLocation = IntArray(2)
                        scrollView.getLocationInWindow(scrollViewLocation)

                        // Calculate how much to scroll
                        val scrollTo = scrollView.scrollY + location[1] - scrollViewLocation[1] - 150

                        scrollView.smoothScrollTo(0, scrollTo.coerceAtLeast(0))
                    }, 350)
                }
            }
        }

        etPCT.setOnEditorActionListener { _, _, _ ->
            hideKeyboard()
            true
        }
    }
    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus()
        }
    }

    private fun loadModel() {
        try {
            env = OrtEnvironment.getEnvironment()
            val modelBytes = assets.open("anemia_model.onnx").readBytes()
            session = env.createSession(modelBytes)
            Log.d("ONNX", "Model loaded. Inputs: ${session.inputNames}, Outputs: ${session.outputNames}")
        } catch (e: Exception) {
            Toast.makeText(this, "Model load error: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("ONNX", "Load error", e)
        }
    }

    private fun validateInputs(): Boolean {
        val fields = listOf(
            etAge, etHemoglobin, etESR, etWBC, etNeutrophils,
            etLymphocytes, etMonocytes, etEosinophils, etBasophils, etRBC,
            etHCT, etMCV, etMCH, etMCHC, etRDWCV, etRDWSD, etPlatelet,
            etMPV, etPDW, etPCT
        )

        for (field in fields) {
            if (field.text.toString().trim().isEmpty()) {
                field.error = "Required"
                field.requestFocus()
                scrollView.smoothScrollTo(0, field.top - 200)
                return false
            }
        }
        return true
    }

    private fun EditText.getFloat(): Float {
        return this.text.toString().trim().toFloat()
    }

    private fun predict() {
        try {
            val inputData = floatArrayOf(
                etAge.getFloat(),
                spSex.selectedItemPosition.toFloat(),
                etHemoglobin.getFloat(),
                etESR.getFloat(),
                etWBC.getFloat(),
                etNeutrophils.getFloat(),
                etLymphocytes.getFloat(),
                etMonocytes.getFloat(),
                etEosinophils.getFloat(),
                etBasophils.getFloat(),
                etRBC.getFloat(),
                etHCT.getFloat(),
                etMCV.getFloat(),
                etMCH.getFloat(),
                etMCHC.getFloat(),
                etRDWCV.getFloat(),
                etRDWSD.getFloat(),
                etPlatelet.getFloat(),
                etMPV.getFloat(),
                etPDW.getFloat(),
                etPCT.getFloat()
            )

            val inputArray = Array(1) { inputData }
            val inputTensor = OnnxTensor.createTensor(env, inputArray)
            val inputName = session.inputNames.iterator().next()

            val results = session.run(mapOf(inputName to inputTensor))
            val chance = extractProbability(results)

            inputTensor.close()
            results.close()

            showResultDialog(chance)

        } catch (e: Exception) {
            Toast.makeText(this, "Prediction error: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("ONNX", "Predict error", e)
        }
    }

    private fun extractProbability(results: OrtSession.Result): Double {
        try {
            val probOutput = results.get(1).value
            if (probOutput is List<*>) {
                val firstRow = probOutput[0]
                if (firstRow is Map<*, *>) {
                    val prob = firstRow[1L] as? Float
                        ?: firstRow[1] as? Float
                        ?: firstRow.values.last() as Float
                    return (prob * 100).toDouble()
                }
            }
            if (probOutput is Array<*>) {
                val firstRow = probOutput[0]
                if (firstRow is Map<*, *>) {
                    val prob = firstRow[1L] as? Float
                        ?: firstRow[1] as? Float
                        ?: firstRow.values.last() as Float
                    return (prob * 100).toDouble()
                }
                if (firstRow is FloatArray) {
                    return (firstRow[1] * 100).toDouble()
                }
            }
        } catch (e: Exception) {
            Log.e("ONNX", "Parse method 1 failed", e)
        }

        try {
            val output = results.get(0).value
            if (output is Array<*> && output[0] is FloatArray) {
                val probs = output[0] as FloatArray
                return (probs[1] * 100).toDouble()
            }
        } catch (e: Exception) {
            Log.e("ONNX", "Parse method 2 failed", e)
        }

        throw Exception("Could not parse model output")
    }

    private fun showResultDialog(chance: Double) {
        val roundedChance = Math.round(chance * 100.0) / 100.0

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_result)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(true)

        val tvPercent = dialog.findViewById<TextView>(R.id.tvPercent)
        val tvPercentSign = dialog.findViewById<TextView>(R.id.tvPercentSign)
        val tvRiskLabel = dialog.findViewById<TextView>(R.id.tvRiskLabel)
        val tvMessage = dialog.findViewById<TextView>(R.id.tvMessage)
        val progressCircle = dialog.findViewById<ProgressBar>(R.id.progressCircle)
        val riskBar = dialog.findViewById<ProgressBar>(R.id.riskBar)
        val btnClose = dialog.findViewById<MaterialButton>(R.id.btnClose)

        val riskColor = getRiskColor(roundedChance)
        val riskText = getRiskLabel(roundedChance)
        val riskBgColor = getRiskBgColor(roundedChance)

        tvPercent.setTextColor(riskColor)
        tvPercentSign.setTextColor(riskColor)
        tvRiskLabel.setTextColor(riskColor)
        tvRiskLabel.text = riskText

        val badgeBg = tvRiskLabel.background as GradientDrawable
        badgeBg.setColor(riskBgColor)

        val colorHex = String.format("#%06X", 0xFFFFFF and riskColor)
        val msg = "Based on CBC report, patient has <b><font color='$colorHex'>" +
                "${String.format("%.1f", roundedChance)}%</font></b> possibility of being <b>Anemic</b>"
        tvMessage.text = Html.fromHtml(msg, Html.FROM_HTML_MODE_LEGACY)

        val circleDrawable = progressCircle.progressDrawable as LayerDrawable
        circleDrawable.findDrawableByLayerId(android.R.id.progress)
            .setColorFilter(riskColor, PorterDuff.Mode.SRC_IN)

        riskBar.progressDrawable.setColorFilter(riskColor, PorterDuff.Mode.SRC_IN)

        ObjectAnimator.ofInt(progressCircle, "progress", 0, roundedChance.toInt()).apply {
            duration = 1500
            interpolator = DecelerateInterpolator()
            start()
        }

        ObjectAnimator.ofInt(riskBar, "progress", 0, roundedChance.toInt()).apply {
            duration = 1500
            interpolator = DecelerateInterpolator()
            start()
        }

        val handler = Handler(Looper.getMainLooper())
        var current = 0.0
        val step = roundedChance / 60.0
        val counter = object : Runnable {
            override fun run() {
                current += step
                if (current >= roundedChance) {
                    tvPercent.text = String.format("%.1f", roundedChance)
                } else {
                    tvPercent.text = String.format("%.1f", current)
                    handler.postDelayed(this, 20)
                }
            }
        }
        handler.postDelayed(counter, 200)

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun getRiskColor(value: Double): Int = when {
        value <= 30 -> Color.parseColor("#0f9d58")
        value <= 50 -> Color.parseColor("#f9ab00")
        value <= 70 -> Color.parseColor("#e8710a")
        else -> Color.parseColor("#db4437")
    }

    private fun getRiskLabel(value: Double): String = when {
        value <= 30 -> "LOW RISK"
        value <= 50 -> "MODERATE RISK"
        value <= 70 -> "HIGH RISK"
        else -> "VERY HIGH RISK"
    }

    private fun getRiskBgColor(value: Double): Int = when {
        value <= 30 -> Color.parseColor("#e6f4ea")
        value <= 50 -> Color.parseColor("#fef7e0")
        value <= 70 -> Color.parseColor("#fce8e6")
        else -> Color.parseColor("#fce8e6")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::session.isInitialized) session.close()
            if (::env.isInitialized) env.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}