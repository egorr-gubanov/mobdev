package lab1.egor.calculator

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: CalculatorViewModel
    private lateinit var tvDisplay: TextView
    private lateinit var tvExpression: TextView
    private lateinit var tvHistory: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[CalculatorViewModel::class.java]
        
        tvDisplay = findViewById(R.id.tvDisplay)
        tvExpression = findViewById(R.id.tvExpression)
        tvHistory = findViewById(R.id.tvHistory)

        viewModel.displayText.observe(this) { tvDisplay.text = it }
        viewModel.expressionText.observe(this) { tvExpression.text = it }
        viewModel.history.observe(this) { historyList ->
            tvHistory.text = historyList.firstOrNull() ?: ""
        }

        setupButtons()
    }

    private fun setupButtons() {
        val digits = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnDot
        )
        digits.forEach { id ->
            findViewById<Button>(id).setOnClickListener { 
                viewModel.onDigitPressed((it as Button).text.toString()) 
            }
        }

        val operations = listOf(
            R.id.btnPlus to "+",
            R.id.btnMinus to "-",
            R.id.btnMultiply to "*",
            R.id.btnDivide to "/"
        )
        operations.forEach { (id, op) ->
            findViewById<Button>(id).setOnClickListener { viewModel.onOperationPressed(op) }
        }

        findViewById<Button>(R.id.btnEquals).setOnClickListener { viewModel.calculateResult(true) }
        findViewById<Button>(R.id.btnClear).setOnClickListener { viewModel.onClearPressed() }
        findViewById<Button>(R.id.btnPercent).setOnClickListener { viewModel.onPercentPressed() }
        findViewById<Button>(R.id.btnNegate).setOnClickListener { viewModel.onNegatePressed() }
    }
}
