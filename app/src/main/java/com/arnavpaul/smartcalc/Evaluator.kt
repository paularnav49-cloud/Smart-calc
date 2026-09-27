package com.arnavpaul.smartcalc

object Evaluator {

    private val OPS = mapOf('+' to 1, '-' to 1, '*' to 2, '/' to 2, '%' to 2)

    fun eval(raw: String): Double {
        var s = raw.replace("×", "*").replace("÷", "/").replace("−", "-")
        s = s.replace(Regex("[^0-9+\-*/%.() ]"), "")
        s = s.replace(Regex("\\s+"), "")
        if (s.isEmpty()) return 0.0
        if (s.startsWith("-")) s = "0" + s
        s = s.replace("(-", "(0-")

        val out = ArrayList<String>()
        val ops = ArrayDeque<Char>()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isDigit() || c == '.' -> {
                    var j = i
                    while (j < s.length && (s[j].isDigit() || s[j] == '.')) j++
                    out.add(s.substring(i, j))
                    i = j
                }
                c == '(' -> { ops.addLast(c); i++ }
                c == ')' -> {
                    while (ops.isNotEmpty() && ops.last() != '(') out.add(ops.removeLast().toString())
                    if (ops.isNotEmpty()) ops.removeLast()
                    i++
                }
                OPS.containsKey(c) -> {
                    while (ops.isNotEmpty() && ops.last() != '(' && (OPS[ops.last()] ?: 0) >= (OPS[c] ?: 0)) {
                        out.add(ops.removeLast().toString())
                    }
                    ops.addLast(c)
                    i++
                }
                else -> i++
            }
        }
        while (ops.isNotEmpty()) {
            val op = ops.removeLast()
            if (op != '(') out.add(op.toString())
        }

        val st = ArrayDeque<Double>()
        for (t in out) {
            val isOp = t.length == 1 && OPS.containsKey(t[0])
            if (isOp) {
                val b = if (st.isEmpty()) 0.0 else st.removeLast()
                val a = if (st.isEmpty()) 0.0 else st.removeLast()
                val r = when (t) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> if (b == 0.0) Double.NaN else a / b
                    else -> a % b
                }
                st.addLast(r)
            } else {
                st.addLast(t.toDoubleOrNull() ?: 0.0)
            }
        }
        return if (st.isEmpty()) 0.0 else st.last()
    }

    fun fmt(d: Double): String {
        if (d.isNaN() || d.isInfinite()) return "Error"
        return if (d == Math.floor(d) && Math.abs(d) < 1e15) {
            d.toLong().toString()
        } else {
            String.format("%.6f", d).trimEnd('0').trimEnd('.')
        }
    }
}
