package com.miguel.unitask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.miguel.unitask.databinding.FragmentRegisterBinding

/**
 * Pantalla de Registro. Aquí es donde los nuevos alumnos crean su cuenta.
 * Validamos que los datos sean correctos antes de mandarlos a Firebase.
 */
class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Acción al pulsar el botón de crear cuenta.
        binding.registerBtn.setOnClickListener {
            val name = binding.registerName.text?.toString()?.trim() ?: ""
            val email = binding.registerEmail.text?.toString()?.trim() ?: ""
            val password = binding.registerPassword.text?.toString()?.trim() ?: ""
            val confirmPassword = binding.registerConfirmPassword.text?.toString()?.trim() ?: ""

            // Usamos una bandera para saber si algo anda mal antes de intentar el registro.
            var hasError = false

            if (name.isEmpty()) {
                binding.registerName.error = "Dinos tu nombre"
                hasError = true
            }
            if (email.isEmpty()) {
                binding.registerEmail.error = "Escribe un correo"
                hasError = true
            }
            if (password.isEmpty()) {
                binding.registerPassword.error = "Crea una contraseña"
                hasError = true
            }
            if (password.length < 6) {
                binding.registerPassword.error = "Debe tener al menos 6 caracteres"
                hasError = true
            }
            if (confirmPassword != password) {
                binding.registerConfirmPassword.error = "Las contraseñas no son iguales"
                hasError = true
            }
            
            // Si hubo errores, detenemos el proceso aquí mismo.
            if (hasError) return@setOnClickListener

            binding.registerBtn.isEnabled = false
            binding.registerBtn.text = "Creando cuenta..."

            // Creamos el usuario en Firebase Auth.
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Si se creó bien, le ponemos su nombre al perfil.
                        auth.currentUser?.updateProfile(
                            UserProfileChangeRequest.Builder().setDisplayName(name).build()
                        )
                        // Lo mandamos al Dashboard sin escalas.
                        findNavController().navigate(R.id.action_register_to_dashboard)
                    } else {
                        binding.registerBtn.isEnabled = true
                        binding.registerBtn.text = getString(R.string.register_btn)
                        val error = task.exception?.localizedMessage ?: "Error al registrarse"
                        binding.registerEmail.error = error
                    }
                }
        }

        // Si ya tiene cuenta, lo regresamos al login.
        binding.registerLoginLink.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
