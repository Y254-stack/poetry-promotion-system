package com.example.poetry

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.NavigationUiSaveStateControl
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.poetry.databinding.ActivityMainBinding
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    @NavigationUiSaveStateControl
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
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController, false)

        setupDraggableFab()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val topLevelDestinations = setOf(
                R.id.homeFragment,
                R.id.learningHubFragment,
                R.id.communitySquareFragment,
                R.id.userCenterFragment
            )
            val aiHiddenDestinations = setOf(
                R.id.aiChatFragment,
                R.id.fillBlankFragment,
                R.id.chainFragment,
                R.id.feihuaFragment,
                R.id.quizFragment,
                R.id.quizResultFragment,
                R.id.learningHubFragment,
                R.id.userCenterFragment,
                R.id.loginFragment,
                R.id.registerFragment,
                R.id.forgotPasswordFragment,
                R.id.changePasswordFragment
            )
            binding.bottomNavigation.visibility =
                if (destination.id in topLevelDestinations) View.VISIBLE else View.GONE

            binding.aiFab.visibility =
                if (destination.id in aiHiddenDestinations) View.GONE else View.VISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun setupDraggableFab() {
        val fab = binding.aiFab
        var dX = 0f
        var dY = 0f
        var initialRawX = 0f
        var initialRawY = 0f
        var hasMoved = false
        var isHiddenToEdge = false

        fab.post {
            snapFabToNearestEdge(hidden = false, animate = false)
        }

        fab.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    initialRawX = event.rawX
                    initialRawY = event.rawY
                    hasMoved = false
                    if (isHiddenToEdge) {
                        revealFabInstantly()
                        isHiddenToEdge = false
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    val parent = view.parent as View
                    val maxX = (parent.width - view.width).toFloat()
                    val maxY = (parent.height - view.height - binding.bottomNavigation.height).toFloat()

                    val newX = (event.rawX + dX).coerceIn(0f, maxX)
                    val minY = binding.toolbar.height.toFloat()
                    val newY = (event.rawY + dY).coerceIn(minY, max(maxY, minY))

                    view.x = newX
                    view.y = newY

                    val deltaX = abs(event.rawX - initialRawX)
                    val deltaY = abs(event.rawY - initialRawY)
                    if (deltaX > 18 || deltaY > 18) {
                        hasMoved = true
                    }
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    if (!hasMoved) {
                        if (isHiddenToEdge) {
                            revealFabAnimated()
                            isHiddenToEdge = false
                        } else {
                            view.performClick()
                            navController.navigate(R.id.aiChatFragment)
                        }
                    } else {
                        val parent = view.parent as View
                        val shouldHide = view.x < 36f || view.x > parent.width - view.width - 36f
                        snapFabToNearestEdge(hidden = shouldHide, animate = true)
                        isHiddenToEdge = shouldHide
                    }
                }
            }
            true
        }
    }

    private fun snapFabToNearestEdge(hidden: Boolean, animate: Boolean) {
        val fab = binding.aiFab
        val parent = fab.parent as View
        val centerX = fab.x + fab.width / 2f
        val anchorRight = centerX > parent.width / 2f
        val visibleMargin = 18f
        val hiddenPeek = fab.width * 0.36f

        val targetX = if (anchorRight) {
            if (hidden) parent.width - hiddenPeek else parent.width - fab.width - visibleMargin
        } else {
            if (hidden) -(fab.width - hiddenPeek) else visibleMargin
        }

        if (!animate) {
            fab.x = targetX
            return
        }

        ValueAnimator.ofFloat(fab.x, targetX).apply {
            duration = 220L
            addUpdateListener { animator ->
                fab.x = animator.animatedValue as Float
            }
            start()
        }
    }

    private fun revealFabInstantly() {
        snapFabToNearestEdge(hidden = false, animate = false)
    }

    private fun revealFabAnimated() {
        snapFabToNearestEdge(hidden = false, animate = true)
    }
}
