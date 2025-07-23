package com.example.example.screens


import android.annotation.SuppressLint
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.example.screens.menu_screens.HelpScreen
import com.example.example.screens.menu_screens.ProfileScreen
import com.example.example.screens.menu_screens.SettingsScreen

enum class AllScreens {
    StartScroll,
    StartLoad
}

enum class MenuScreens {
    ChatsPagingScreen,
    ProfileScreen,
    SettingsScreen,
    HelpScreen
}

@Composable
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
fun GeneralScreen(navController: NavHostController = rememberNavController()) {
    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AllScreens.StartScroll.name,
        ) {
            composable(route = AllScreens.StartScroll.name) {
                WelcomeScreen(onNextButtonClickedToSkip = {
                    navController.navigate(
                        AllScreens.StartLoad.name
                    )
                })
            }

            composable(route = AllScreens.StartLoad.name) {
                ChatsPagingScreen(onNextButtonClickedToGo1 = {
                    navController.navigate(
                        MenuScreens.ProfileScreen.name
                    )
                },
                    onNextButtonClickedToGo2 = {
                        navController.navigate(
                            MenuScreens.SettingsScreen.name
                        )
                    },
                    onNextButtonClickedToGo3 = {
                        navController.navigate(
                            MenuScreens.HelpScreen.name
                        )
                    })
            }


            composable(route = MenuScreens.ProfileScreen.name) {
                ProfileScreen()
            }
            composable(route = MenuScreens.SettingsScreen.name) {
                SettingsScreen()
            }
            composable(route = MenuScreens.HelpScreen.name) {
                HelpScreen()
            }

        }
        /*NavHost(navController = navController, startDestination = MenuScreens.ChatsPagingScreen.name) {
            composable(route = MenuScreens.ChatsPagingScreen.name) {
                ChatsPagingScreen()
            }



        }*/

    }
}