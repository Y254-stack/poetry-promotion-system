package com.example.poetry

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.NavigationUiSaveStateControl
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.poetry.databinding.ActivityMainBinding

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
        // 关闭底栏多返回栈保存，避免从发帖等子页返回后点击其它 Tab 仍停在社区页
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController, false)

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
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
