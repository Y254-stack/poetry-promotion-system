package com.example.poetry

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.poetry.databinding.ActivityMainBinding
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.learningHubFragment,
                R.id.communitySquareFragment,
                R.id.userCenterFragment
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNavigation.setupWithNavController(navController)

        setupDraggableFab()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val topLevelDestinations = setOf(
                R.id.homeFragment,
                R.id.learningHubFragment,
                R.id.communitySquareFragment,
                R.id.userCenterFragment
            )
            binding.bottomNavigation.visibility =
                if (destination.id in topLevelDestinations) android.view.View.VISIBLE
                else android.view.View.GONE

            binding.aiFab.visibility =
                if (destination.id == R.id.aiChatFragment) android.view.View.GONE
                else android.view.View.VISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun setupDraggableFab() {
        var dX = 0f
        var dY = 0f
        var initialX = 0f
        var initialY = 0f
        var hasMoved = false

        binding.aiFab.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    initialX = event.rawX
                    initialY = event.rawY
                    hasMoved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = event.rawX + dX
                    val newY = event.rawY + dY
                    view.x = newX
                    view.y = newY

                    val deltaX = abs(event.rawX - initialX)
                    val deltaY = abs(event.rawY - initialY)
                    if (deltaX > 20 || deltaY > 20) {
                        hasMoved = true
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (!hasMoved) {
                        view.performClick()
                        navController.navigate(R.id.aiChatFragment)
                    }
                }
            }
            true
        }
    }
}
