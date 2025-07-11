package com.example.example.screens


import android.annotation.SuppressLint
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

enum class AllScreens {
    StartScroll,
    StartLoad,
    Character,
    MoreCharacter,
    Location
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
                ChatsPagingScreen()
            }

        }

    }
}