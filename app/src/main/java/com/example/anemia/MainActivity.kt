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
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    // Forest model data
    private lateinit var forest: List<TreeNode>

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

    // Tree structure
    data class TreeNode(
        val feature: IntArray,
        val threshold: DoubleArray,
        val childrenLeft: IntArray,
        val childrenRight: IntArray,
        val value: Array<IntArray>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadForestModel()
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


    //  LOAD JSON FOREST MODEL
    private fun loadForestModel() {
        try {
            val jsonString = assets.open("anemia_forest.json")
                .bufferedReader().use { it.readText() }

            val jsonArray = JSONArray(jsonString)
            val trees = mutableListOf<TreeNode>()

            for (i in 0 until jsonArray.length()) {
                val tree = jsonArray.getJSONObject(i)

                val feature = jsonArrayToIntArray(tree.getJSONArray("feature"))
                val threshold = jsonArrayToDoubleArray(tree.getJSONArray("threshold"))
                val childrenLeft = jsonArrayToIntArray(tree.getJSONArray("children_left"))
                val childrenRight = jsonArrayToIntArray(tree.getJSONArray("children_right"))

                val valueJson = tree.getJSONArray("value")
                val value = Array(valueJson.length()) { j ->
                    val pair = valueJson.getJSONArray(j)
                    intArrayOf(pair.getInt(0), pair.getInt(1))
                }

                trees.add(TreeNode(feature, threshold, childrenLeft, childrenRight, value))
            }

            forest = trees
            Log.d("MODEL", "✅ Loaded ${forest.size} trees")

        } catch (e: Exception) {
            Toast.makeText(this, "Model load error: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("MODEL", "Error", e)
        }
    }

    private fun jsonArrayToIntArray(arr: JSONArray): IntArray {
        return IntArray(arr.length()) { arr.getInt(it) }
    }

    private fun jsonArrayToDoubleArray(arr: JSONArray): DoubleArray {
        return DoubleArray(arr.length()) { arr.getDouble(it) }
    }

    //  PREDICT — RUN FOREST MANUALLY
    private fun predictTree(tree: TreeNode, features: DoubleArray): DoubleArray {
        var nodeId = 0

        while (tree.childrenLeft[nodeId] != -1) {  // -1 = leaf node
            val featureIndex = tree.feature[nodeId]
            val threshold = tree.threshold[nodeId]

            nodeId = if (features[featureIndex] <= threshold) {
                tree.childrenLeft[nodeId]
            } else {
                tree.childrenRight[nodeId]
            }
        }

        // Leaf node — return [notAnemic, anemic] counts
        val counts = tree.value[nodeId]
        val total = counts[0] + counts[1]
        return doubleArrayOf(
            counts[0].toDouble() / total,
            counts[1].toDouble() / total
        )
    }

    private fun predictForest(features: DoubleArray): Double {
        var totalProb = 0.0

        for (tree in forest) {
            val probs = predictTree(tree, features)
            totalProb += probs[1]  // anemia probability
        }

        // Average across all trees
        return (totalProb / forest.size) * 100
    }


    //  KEYBOARD HANDLING
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
                        val location = IntArray(2)
                        view.getLocationInWindow(location)
                        val svLoc = IntArray(2)
                        scrollView.getLocationInWindow(svLoc)
                        val scrollTo = scrollView.scrollY + location[1] - svLoc[1] - 150
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


    //  VALIDATION & PREDICT
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
                val location = IntArray(2)
                field.getLocationInWindow(location)
                val svLoc = IntArray(2)
                scrollView.getLocationInWindow(svLoc)
                scrollView.smoothScrollTo(0, (scrollView.scrollY + location[1] - svLoc[1] - 150).coerceAtLeast(0))
                return false
            }
        }
        return true
    }

    private fun EditText.getDouble(): Double {
        return this.text.toString().trim().toDouble()
    }

    private fun predict() {
        try {
            // 21 features — same order as training
            val features = doubleArrayOf(
                etAge.getDouble(),
                spSex.selectedItemPosition.toDouble(),
                etHemoglobin.getDouble(),
                etESR.getDouble(),
                etWBC.getDouble(),
                etNeutrophils.getDouble(),
                etLymphocytes.getDouble(),
                etMonocytes.getDouble(),
                etEosinophils.getDouble(),
                etBasophils.getDouble(),
                etRBC.getDouble(),
                etHCT.getDouble(),
                etMCV.getDouble(),
                etMCH.getDouble(),
                etMCHC.getDouble(),
                etRDWCV.getDouble(),
                etRDWSD.getDouble(),
                etPlatelet.getDouble(),
                etMPV.getDouble(),
                etPDW.getDouble(),
                etPCT.getDouble()
            )

            val chance = predictForest(features)
            val roundedChance = Math.round(chance * 100.0) / 100.0

            showResultDialog(roundedChance)

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("PREDICT", "Error", e)
        }
    }

    //  RESULT DIALOG
    private fun showResultDialog(chance: Double) {
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

        val riskColor = getRiskColor(chance)
        val riskText = getRiskLabel(chance)
        val riskBgColor = getRiskBgColor(chance)

        tvPercent.setTextColor(riskColor)
        tvPercentSign.setTextColor(riskColor)
        tvRiskLabel.setTextColor(riskColor)
        tvRiskLabel.text = riskText

        val badgeBg = tvRiskLabel.background as GradientDrawable
        badgeBg.setColor(riskBgColor)

        val colorHex = String.format("#%06X", 0xFFFFFF and riskColor)
        val msg = "Based on CBC report, patient has <b><font color='$colorHex'>" +
                "${String.format("%.1f", chance)}%</font></b> possibility of being <b>Anemic</b>"
        tvMessage.text = Html.fromHtml(msg, Html.FROM_HTML_MODE_LEGACY)

        val circleDrawable = progressCircle.progressDrawable as LayerDrawable
        circleDrawable.findDrawableByLayerId(android.R.id.progress)
            .setColorFilter(riskColor, PorterDuff.Mode.SRC_IN)

        riskBar.progressDrawable.setColorFilter(riskColor, PorterDuff.Mode.SRC_IN)

        ObjectAnimator.ofInt(progressCircle, "progress", 0, chance.toInt()).apply {
            duration = 1500
            interpolator = DecelerateInterpolator()
            start()
        }

        ObjectAnimator.ofInt(riskBar, "progress", 0, chance.toInt()).apply {
            duration = 1500
            interpolator = DecelerateInterpolator()
            start()
        }

        val handler = Handler(Looper.getMainLooper())
        var current = 0.0
        val step = chance / 60.0
        val counter = object : Runnable {
            override fun run() {
                current += step
                if (current >= chance) {
                    tvPercent.text = String.format("%.1f", chance)
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
}