package com.example.geouax.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.TextView
import com.example.geouax.R

class PerfilFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_perfil, container, false)

        val textPerfil = view.findViewById<TextView>(R.id.textPerfil)
        // Puedes usar textPerfil.text = "Perfil de usuario" si quieres modificar algo aquí

        return view
    }
}
