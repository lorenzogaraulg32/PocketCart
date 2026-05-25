package it.uniupo.ebay_clone

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.Fragment


class CreditCardSimulator : Fragment() {

    private var listener: CreditCardSimulatorListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val button = view.findViewById<Button>(R.id.ricarica_btn)

        button?.setOnClickListener {
            view.findViewById<TextView>(R.id.num_carta_err)?.visibility = View.INVISIBLE
            view.findViewById<TextView>(R.id.cvv_err)?.visibility = View.INVISIBLE
            view.findViewById<TextView>(R.id.data_scadenza_err)?.visibility = View.INVISIBLE
            if (checkCreditCard()) {
                val ammount =
                    view.findViewById<EditText>(R.id.amount_input).text.toString().toDouble()
                listener?.sendAmmount(ammount)
                Toast.makeText(requireContext(), "Recharged: $ammount", Toast.LENGTH_LONG)
                    .show()
                parentFragmentManager.beginTransaction().remove(this).commit()
            } else {
                Toast.makeText(requireContext(), "Credit Card Error", Toast.LENGTH_LONG)
                    .show()
            }
        }

        val dataField = view.findViewById<EditText>(R.id.data_scadenza_field)
        dataField.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //check user 2 ha scritto 2 caratteri
                if (s?.length == 2 && before == 0) {
                    val updateText = "${s}/"
                    //aggiorno il testo
                    dataField.setText(updateText)
                    //sposto il puntatore alla fine della stringa
                    dataField.setSelection(updateText.length)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                //nothing
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //nothing
            }
        })


        val cardNum = view.findViewById<EditText>(R.id.credit_card_num_field)
        val textWathcer = object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                if (before == 0 && s?.length == 4 || before == 0 && s?.length == 9 || before == 0 && s?.length == 14) {
                    val updatedText = "${s}-"
                    cardNum.setText(updatedText)
                    cardNum.setSelection(updatedText.length)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                //nothing
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //nothing
            }
        }
        cardNum.addTextChangedListener(textWathcer)


        view.findViewById<ImageButton>(R.id.back_btn).setOnClickListener {
            parentFragmentManager.beginTransaction().remove(this).commitNow()
        }
    }

    private fun checkCreditCard(): Boolean {
        val num_card =
            view?.findViewById<EditText>(R.id.credit_card_num_field)?.text.toString()
        var checkNum = false
        if (num_card.isEmpty()) {
            checkNum = false

            view?.findViewById<TextView>(R.id.num_carta_err)?.visibility = View.VISIBLE
        } else if (num_card.length == 19) {
            val count = num_card.count { it == '-' }
            if (count == 3) {
                checkNum = true
            } else {
                checkNum = false
                view?.findViewById<TextView>(R.id.num_carta_err)?.visibility = View.VISIBLE
            }
        } else {
            view?.findViewById<TextView>(R.id.num_carta_err)?.visibility = View.VISIBLE
        }
        val data_scadenza = view?.findViewById<EditText>(R.id.data_scadenza_field)?.text.toString()
        var checkData = false
        if (data_scadenza.isEmpty()) {
            checkData = false
            view?.findViewById<TextView>(R.id.data_scadenza_err)?.visibility = View.VISIBLE
        } else if (data_scadenza.toList().size == 5 && data_scadenza.contains("/")) {
            checkData = true
        } else {

            view?.findViewById<TextView>(R.id.data_scadenza_err)?.visibility = View.VISIBLE
        }
        val cvv = view?.findViewById<EditText>(R.id.ccv_field)?.text.toString()
        var checkcvv = false
        if (cvv.isEmpty()) {
            checkcvv = false
            view?.findViewById<TextView>(R.id.cvv_err)?.visibility = View.VISIBLE
        } else if (cvv.toList().size == 3 && cvv.isDigitsOnly()) {
            checkcvv = true
        } else {
            view?.findViewById<TextView>(R.id.cvv_err)?.visibility = View.VISIBLE
        }
        if (checkData && checkNum && checkcvv) {
            return true
        }
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_credit_card_simulator, container, false)
    }

    interface CreditCardSimulatorListener {
        fun sendAmmount(ammount: Double)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CreditCardSimulatorListener) {
            listener = context
        }
    }

}