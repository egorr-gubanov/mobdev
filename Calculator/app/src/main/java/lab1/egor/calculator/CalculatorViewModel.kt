package lab1.egor.calculator

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CalculatorViewModel : ViewModel() {

    private val _displayText = MutableLiveData("0")
    val displayText: LiveData<String> = _displayText

    private val _expressionText = MutableLiveData("")
    val expressionText: LiveData<String> = _expressionText

    private val _history = MutableLiveData<List<String>>(emptyList())
    val history: LiveData<List<String>> = _history

    private var currentInput = ""
    private var previousInput = ""
    private var currentOperation = ""
    private var isNewOp = false

    fun onDigitPressed(digit: String) {
        if (isNewOp) {
            currentInput = ""
            isNewOp = false
        }
        
        if (digit == "." && currentInput.contains(".")) return
        if (currentInput == "0" && digit != ".") {
            currentInput = digit
        } else {
            currentInput += digit
        }
        updateDisplay()
    }

    fun onOperationPressed(operation: String) {
        if (currentInput.isNotEmpty()) {
            if (previousInput.isNotEmpty() && currentOperation.isNotEmpty()) {
                calculateResult(false)
            } else {
                previousInput = currentInput
            }
            currentInput = ""
        } else if (previousInput.isEmpty() && _displayText.value != "0") {
            previousInput = _displayText.value ?: ""
        }
        
        currentOperation = operation
        isNewOp = false
        updateExpression()
        updateDisplay()
    }

    fun calculateResult(isFinal: Boolean = true) {
        val secondOperand = if (currentInput.isEmpty()) previousInput else currentInput
        if (previousInput.isEmpty() || currentOperation.isEmpty()) return

        try {
            val num1 = previousInput.toDouble()
            val num2 = secondOperand.toDouble()
            var result = 0.0

            when (currentOperation) {
                "+" -> result = num1 + num2
                "-" -> result = num1 - num2
                "*" -> result = num1 * num2
                "/" -> {
                    if (num2 == 0.0) {
                        _displayText.value = "Ошибка"
                        resetAll()
                        return
                    }
                    result = num1 / num2
                }
            }

            val resultStr = formatResult(result)
            
            if (isFinal) {
                val fullExpression = "$previousInput $currentOperation $secondOperand = $resultStr"
                addToHistory(fullExpression)
                
                _displayText.value = resultStr
                currentInput = "" 
                previousInput = resultStr
                currentOperation = ""
                _expressionText.value = ""
                isNewOp = true
            } else {
                previousInput = resultStr
                updateDisplay()
            }
        } catch (e: Exception) {
            _displayText.value = "Ошибка"
            resetAll()
        }
    }

    fun onPercentPressed() {
        if (currentInput.isNotEmpty()) {
            val value = currentInput.toDouble() / 100
            currentInput = formatResult(value)
            updateDisplay()
        }
    }

    fun onNegatePressed() {
        if (currentInput.isNotEmpty() && currentInput != "0") {
            currentInput = if (currentInput.startsWith("-")) {
                currentInput.substring(1)
            } else {
                "-$currentInput"
            }
            updateDisplay()
        }
    }

    fun onClearPressed() {
        resetAll()
        updateDisplay()
        updateExpression()
    }

    private fun resetAll() {
        currentInput = ""
        previousInput = ""
        currentOperation = ""
        _expressionText.value = ""
        isNewOp = false
    }

    private fun formatResult(result: Double): String {
        return if (result % 1.0 == 0.0) {
            result.toLong().toString()
        } else {
            result.toString()
        }
    }

    private fun updateDisplay() {
        _displayText.value = if (currentInput.isEmpty()) "0" else currentInput
    }

    private fun updateExpression() {
        _expressionText.value = if (currentOperation.isNotEmpty()) {
            "$previousInput $currentOperation"
        } else {
            ""
        }
    }

    private fun addToHistory(item: String) {
        val currentList = _history.value.orEmpty().toMutableList()
        currentList.add(0, item)
        if (currentList.size > 10) currentList.removeAt(currentList.size - 1)
        _history.value = currentList
    }
}
