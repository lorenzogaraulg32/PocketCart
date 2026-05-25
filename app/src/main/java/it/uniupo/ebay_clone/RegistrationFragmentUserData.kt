package it.uniupo.ebay_clone

import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RegistrationFragmentUserData : Fragment() {

    var nome = ""
    var email = ""
    var username = ""
    var password = ""
    var confPass = ""
    var code = 0

    var readyToReg = false

    var db = FirebaseFirestore.getInstance()
    private lateinit var res: RegistrationFragmentUserData.RegResult

    interface RegResult {
        fun getResults(
            nome: String,
            email: String,
            username: String,
            password: String,
            esito: Boolean
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            res = context as RegResult
        } catch (e: Exception) {
            println("REG Exception: $e")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        code = arguments?.getString("code").toString().toInt()
        println("Codice = $code")
        if (code == 1) {
            MainScope().launch {
                withContext(Dispatchers.Main) {
                    readyToReg = errorManager(nome, email, username, password, confPass)
                    if (readyToReg) {
                        println("Ready to register, chiamo reg")
                        res.getResults(nome, email, username, password, true)
                    } else {
                        println("Qualche check non funzia")
                        res.getResults(nome, email, username, password, false)
                    }
                }
            }
        }


        val fullnameView = view.findViewById<TextInputEditText>(R.id.reg_full_name_field)
        val emailView = view.findViewById<TextInputEditText>(R.id.reg_email_field)
        val usernameView = view.findViewById<TextInputEditText>(R.id.reg_username_field)
        val passwordView = view.findViewById<TextInputEditText>(R.id.reg_pass_field)
        val confPassView = view.findViewById<TextInputEditText>(R.id.reg_conf_pass_field)

        view.findViewById<FrameLayout>(R.id.layout1).setOnClickListener {
            it.hideKeyboard()
        }


        fullnameView.onFocusChangeListener =
            View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    nome = fullnameView.text.toString()
                }
            }

        emailView.onFocusChangeListener =
            View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    email = emailView.text.toString()
                }
            }

        usernameView.onFocusChangeListener =
            View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    username = usernameView.text.toString()
                }
            }

        passwordView.onFocusChangeListener =
            View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    password = passwordView.text.toString()
                }
            }

        confPassView.onFocusChangeListener =
            View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    confPass = confPassView.text.toString()
                }
            }
    }

    fun hasSpecialCharacters(input: String): Boolean {
        val specialCharactersRegex = Regex("[A-Za-z0-9]+")
        return specialCharactersRegex.containsMatchIn(input)
    }

    private fun View.hideKeyboard() {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }

    private suspend fun errorManager(
        nome: String,
        email: String,
        username: String,
        password: String,
        confPass: String
    ): Boolean {

        val passwordErr: TextView? = view?.findViewById<TextView>(R.id.pass_err)
        val confPassErr: TextView? = view?.findViewById<TextView>(R.id.conf_pass_err)
        var checkPassword = false
        var checkConfPassword = false

        if (!checkFullName(nome)) {
            return false
        }
        if (!checkEmail(email)) {
            return false
        }
        if (!checkUsername(username)) {
            return false
        }

        //password fatto
        when {
            password.isEmpty() -> {
                passwordErr?.text = getString(R.string.err_not_empty)
                passwordErr?.visibility = View.VISIBLE
                return false
            }

            password.isNotEmpty() -> {
                passwordErr?.visibility = View.INVISIBLE
                checkPassword = checkPass(password)
            }
        }
        //confpass fatto
        when {
            checkPassword == false -> {
                confPassErr?.visibility = View.INVISIBLE
                return false
            }

            !password.equals(confPass) -> {
                confPassErr?.text = getString(R.string.pass_not_match_err)
                confPassErr?.visibility = View.VISIBLE
                return false
            }

            password.equals(confPass) -> {
                checkConfPassword = true
            }
        }

        if (checkFullName(nome) && checkEmail(email) && checkUsername(username) && checkPassword && checkConfPassword) {
            return true
        } else {
            return false
        }

    }

    private fun checkPass(pass: String): Boolean {
        var checkMaiuscola = false
        var checkNumero = false
        val passwordErr = view?.findViewById<TextView>(R.id.pass_err)

        when {
            pass.length < 8 -> {
                passwordErr?.text = getString(R.string.pass_err_length)
                passwordErr?.visibility = View.VISIBLE
                return false
            }
        }

        for (c in pass) {
            if (c.isUpperCase()) {
                checkMaiuscola = true
                break
            }
        }
        if (!checkMaiuscola) {
            passwordErr?.text = getString(R.string.pass_err_upper)
            passwordErr?.visibility = View.VISIBLE
            return false
        }

        for (c in pass) {
            if (c.isDigit()) {
                checkNumero = true
                break
            }
        }
        if (!checkNumero) {
            passwordErr?.text = getString(R.string.pass_err_digit)
            passwordErr?.visibility = View.VISIBLE
            return false
        }
        return true
    }

    private suspend fun checkEmail(email: String): Boolean {
        val emailErr = view?.findViewById<TextView>(R.id.email_err)

        when {
            email.isEmpty() -> {
                emailErr?.text = getString(R.string.err_not_empty)
                emailErr?.visibility = View.VISIBLE
                return false
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailErr?.text = getString(R.string.log_err_email_format)
                emailErr?.visibility = View.VISIBLE
                return false
            }

            else -> {
                val query = db.collection("User").get().await()
                //è necessario dispatchers.main perchè altrimenti c'è un errore con i thread, solo il thread main( che ha creato la ref della view) può modificarla
                return withContext(Dispatchers.Main) {
                    for (doc in query.documents) {
                        if (doc.getString("Email") == email) {
                            emailErr?.text = getString(R.string.reg_err_email_already)
                            emailErr?.visibility = View.VISIBLE
                            println("trovato Email")
                            return@withContext false
                        }
                    }
                    println("non trovato Email")
                    true
                }
            }
        }
    }

    private suspend fun checkUsername(username: String): Boolean {
        val usernameErr = view?.findViewById<TextView>(R.id.username_err)
        when {
            username.isEmpty() -> {
                usernameErr?.text = getString(R.string.err_not_empty)
                usernameErr?.visibility = View.VISIBLE
                return false
            }

            username.length < 3 || username.length > 12 -> {
                usernameErr?.text = getString(R.string.reg_username_length_err)
                usernameErr?.visibility = View.VISIBLE
                return false
            }

            !hasSpecialCharacters(username) -> {
                usernameErr?.text = getString(R.string.reg_no_spec_char)
                usernameErr?.visibility = View.VISIBLE
                return false
            }

            else -> {
                val query = db.collection("User").get().await()
                //è necessario dispatchers.main perchè altrimenti c'è un errore con i thread, solo il thread main( che ha creato la ref della view) può modificarla
                return withContext(Dispatchers.Main) {
                    for (doc in query.documents) {
                        if (doc.getString("Username") == username) {
                            usernameErr?.text = getString(R.string.reg_username_already_err)
                            usernameErr?.visibility = View.VISIBLE
                            return@withContext false
                        }
                    }
                    true
                }
            }
        }
    }

    //fatto
    private fun checkFullName(fullname: String): Boolean {
        val nameErr = view?.findViewById<TextView>(R.id.name_err)
        when {
            fullname.isEmpty() -> {
                nameErr?.text = getString(R.string.err_not_empty)
                nameErr?.visibility = View.VISIBLE
                println("fullname check fallito")
                return false
            }

            fullname.length < 3 -> {
                nameErr?.text = getString(R.string.reg_name_short_err)
                nameErr?.visibility = View.VISIBLE
                return false
            }

            !hasSpecialCharacters(fullname) -> {
                nameErr?.text = getString(R.string.reg_no_spec_char)
                nameErr?.visibility = View.VISIBLE
                return false
            }
        }
        println("check Username finito")
        return true
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_registratio_user_data, container, false)
    }


}


