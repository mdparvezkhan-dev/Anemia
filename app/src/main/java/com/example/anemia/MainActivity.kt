package com.example.anemia

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.Window
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadModel()

        btnPredict.setOnClickListener {
            if (validateInputs()) {
                predict()
            }
        }
    }

    private fun initViews() {
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

    private fun loadModel() {
        try {
            env = OrtEnvironment.getEnvironment()

            val modelBytes = assets.open("anemia_model.onnx").readBytes()
            session = env.createSession(modelBytes)

            // Debug: Print model input/output info
            Log.d("ONNX", "Input names: ${session.inputNames}")
            Log.d("ONNX", "Input info: ${session.inputInfo}")
            Log.d("ONNX", "Output names: ${session.outputNames}")
            Log.d("ONNX", "Output info: ${session.outputInfo}")

        } catch (e: Exception) {
            Toast.makeText(this, "Model load error: ${e.message}",
                Toast.LENGTH_LONG).show()
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
            // 21 features — same order as training
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

            // Create tensor — 2D array format
            val inputArray = Array(1) { inputData }
            val inputTensor = OnnxTensor.createTensor(env, inputArray)

            // Get input name from model
            val inputName = session.inputNames.iterator().next()
            Log.d("ONNX", "Using input name: $inputName")

            // Run inference
            val results = session.run(mapOf(inputName to inputTensor))

            // Debug: check output types
            for (i in 0 until results.size()) {
                val output = results.get(i)
                Log.d("ONNX", "Output[$i] type: ${output.type}, value class: ${output.value.javaClass.name}")
            }

            // Parse probability
            val chance = extractProbability(results)

            inputTensor.close()
            results.close()

            showResultDialog(chance)

        } catch (e: Exception) {
            Toast.makeText(this, "Prediction error: ${e.message}",
                Toast.LENGTH_LONG).show()
            Log.e("ONNX", "Predict error", e)
        }
    }

    private fun extractProbability(results: OrtSession.Result): Double {
        try {
            // Logcat showed: Output[1] type: ONNX_TYPE_SEQUENCE, value class: java.util.Collections$UnmodifiableRandomAccessList
            // This usually corresponds to List<Map<Long, Float>> for ZipMap in sklearn-onnx
            val probOutput = results.get(1).value
            
            if (probOutput is List<*>) {
                val firstRow = probOutput[0]
                if (firstRow is Map<*, *>) {
                    // Try to get class 1 (Anemia) probability
                    // Map keys can be Long, Int, or String depending on the model's ZipMap configuration
                    val prob = (firstRow[1L] ?: firstRow[1] ?: firstRow["1"] ?: firstRow[1.toInt()]) as? Float
                    
                    if (prob != null) {
                        return (prob * 100).toDouble()
                    }
                    
                    // Fallback: if there are 2 classes, the second one is usually the positive class
                    val values = firstRow.values.toList()
                    if (values.size >= 2) {
                        val p1 = values[1] as? Float
                        if (p1 != null) return (p1 * 100).toDouble()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ONNX", "Sequence parsing failed: ${e.message}")
        }

        try {
            // Alternate Method: Output is 2D float array [[prob_0, prob_1]]
            val output = results.get(1).value
            if (output is Array<*> && output[0] is FloatArray) {
                val probs = output[0] as FloatArray
                if (probs.size >= 2) return (probs[1] * 100).toDouble()
            }
        } catch (e: Exception) {
            Log.e("ONNX", "Float array parsing failed: ${e.message}")
        }

        throw Exception("Could not parse model output. Check Logcat for ONNX debug info.")
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

        // Set colors
        tvPercent.setTextColor(riskColor)
        tvPercentSign.setTextColor(riskColor)
        tvRiskLabel.setTextColor(riskColor)
        tvRiskLabel.text = riskText

        // Badge background
        val badgeBg = tvRiskLabel.background as GradientDrawable
        badgeBg.setColor(riskBgColor)

        // Message
        val colorHex = String.format("#%06X", 0xFFFFFF and riskColor)
        val msg = "Based on CBC report, patient has <b><font color='$colorHex'>" +
                "${String.format("%.1f", roundedChance)}%</font></b> possibility of being <b>Anemic</b>"
        tvMessage.text = Html.fromHtml(msg, Html.FROM_HTML_MODE_LEGACY)

        tvPercent.text = String.format("%.0f", roundedChance)
        progressCircle.progress = roundedChance.toInt()
        riskBar.progress = roundedChance.toInt()

        // Change risk bar color
        riskBar.progressTintList = android.content.res.ColorStateList.valueOf(riskColor)

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getRiskColor(chance: Double): Int {
        return when {
            chance < 30 -> Color.parseColor("#4CAF50") // Green
            chance < 70 -> Color.parseColor("#FF9800") // Orange
            else -> Color.parseColor("#F44336") // Red
        }
    }

    private fun getRiskBgColor(chance: Double): Int {
        return when {
            chance < 30 -> Color.parseColor("#E8F5E9")
            chance < 70 -> Color.parseColor("#FFF3E0")
            else -> Color.parseColor("#FFEBEE")
        }
    }

    private fun getRiskLabel(chance: Double): String {
        return when {
            chance < 30 -> "LOW RISK"
            chance < 70 -> "MEDIUM RISK"
            else -> "HIGH RISK"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::session.isInitialized) session.close()
        if (::env.isInitialized) env.close()
    }
}
