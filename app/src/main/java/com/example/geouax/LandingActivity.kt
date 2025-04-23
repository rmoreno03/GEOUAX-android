package com.example.geouax

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class LandingActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Obtener NavController correctamente
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Conectar BottomNavigationView con NavController
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    if (navController.currentDestination?.id != R.id.homeFragment)
                        navController.navigate(R.id.homeFragment)
                    true
                }
                R.id.puntoFragment -> {
                    if (navController.currentDestination?.id != R.id.puntoFragment)
                        navController.navigate(R.id.puntoFragment)
                    true
                }
                R.id.mapaFragment -> {
                    if (navController.currentDestination?.id != R.id.mapaFragment)
                        navController.navigate(R.id.mapaFragment)
                    true
                }
                R.id.perfilFragment -> {
                    if (navController.currentDestination?.id != R.id.perfilFragment)
                        navController.navigate(R.id.perfilFragment)
                    true
                }
                else -> false
            }
        }

    }
}
