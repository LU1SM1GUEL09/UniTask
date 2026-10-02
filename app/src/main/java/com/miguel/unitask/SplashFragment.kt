package com.miguel.unitask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.miguel.unitask.databinding.FragmentSplashBinding

/**
 * Fragmento de Splash (Pantalla de carga). 
 * Es la primera pantalla que se ve al abrir la app.
 */
class SplashFragment : Fragment() {
    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Usamos postDelayed para esperar 2 segundos antes de saltar a la siguiente pantalla.
        // Esto le da tiempo al usuario de ver el logo y a Firebase de cargar la sesión.
        binding.root.postDelayed({
            // Revisamos si ya hay un usuario logueado en este dispositivo.
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                // Si el usuario ya existe, lo mandamos directo al Dashboard.
                findNavController().navigate(R.id.action_splash_to_dashboard)
            } else {
                // Si es la primera vez o cerró sesión, lo mandamos al Login.
                // Usamos la acción del NavGraph para manejar la navegación limpia.
                findNavController().navigate(R.id.action_splash_to_login)
            }
        }, 2000) // 2000 milisegundos = 2 segundos de logo.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
