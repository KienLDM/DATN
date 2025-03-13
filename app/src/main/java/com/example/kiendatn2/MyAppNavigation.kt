package com.example.kiendatn2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun MyAppNavigation(modifier: Modifier = Modifier, authViewModel: AuthViewModel) {
   val navController = rememberNavController()
   val postViewModel: PostViewModel = viewModel()
   val navBackStackEntry by navController.currentBackStackEntryAsState()
   val currentPostState by postViewModel.currentPostState.observeAsState()

   // Handle navigation to comments screen when a post is selected
   LaunchedEffect(key1 = currentPostState) {
       if (currentPostState is CurrentPostState.PostLoaded && 
           navBackStackEntry?.destination?.route != "comments") {
           navController.navigate("comments") {
               launchSingleTop = true
               // Avoid nested navigation stacks
               popUpTo("home")
           }
       }
   }

   NavHost(navController = navController, startDestination = "login", builder = {
       composable("login") {
           LoginPage(modifier, navController, authViewModel)
       }

       composable("signup") {
           SignupPage(modifier, navController, authViewModel)
       }

       composable("home") {
           HomePage(modifier, navController, authViewModel, postViewModel)
       }
       
       composable("comments") {
           CommentScreen(
               postViewModel = postViewModel,
               onBackClick = { navController.navigateUp() }
           )
       }
   })
}