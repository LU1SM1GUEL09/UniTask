package com.miguel.unitask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.miguel.unitask.databinding.FragmentLoginBinding

/**
 * Pantalla de Inicio de Sesión. Es la puerta de entrada para los usuarios registrados.
 * Usamos Firebase Auth para que el proceso sea seguro y no tengamos que guardar contraseñas nosotros.
 */
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    // Instancia de autenticación de Firebase.
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Acción al pulsar el botón de iniciar sesión.
        binding.loginBtn.setOnClickListener {
            val email = binding.loginEmail.text?.toString()?.trim() ?: ""
            val password = binding.loginPassword.text?.toString()?.trim() ?: ""

            // Validamos que no dejen los campos vacíos.
            if (email.isEmpty()) {
                binding.loginEmail.error = "Escribe tu correo"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.loginPassword.error = "Escribe tu contraseña"
                return@setOnClickListener
            }

            // Cambiamos el estado del botón para que el usuario sepa que la app está trabajando.
            binding.loginBtn.isEnabled = false
            binding.loginBtn.text = "Iniciando..."

            // Intentamos loguear con Firebase.
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    binding.loginBtn.isEnabled = true
                    binding.loginBtn.text = getString(R.string.login_btn)
                    
                    if (task.isSuccessful) {
                        // Si todo sale bien, saltamos al Dashboard.
                        findNavController().navigate(R.id.action_login_to_dashboard)
                    } else {
                        // Si algo falla, le decimos al usuario exactamente qué pasó.
                        val error = when (task.exception) {
                            is FirebaseAuthInvalidUserException -> "El correo no está registrado"
                            is FirebaseAuthInvalidCredentialsException -> "La contraseña no es correcta"
                            else -> task.exception?.localizedMessage ?: "Hubo un error al entrar"
                        }
                        binding.loginEmail.error = error
                    }
                }
        }

        // Link para ir a la pantalla de registro si no tiene cuenta.
        binding.loginRegisterLink.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
