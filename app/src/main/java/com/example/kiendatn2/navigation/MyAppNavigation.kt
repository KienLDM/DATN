package com.example.kiendatn2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kiendatn2.ui.comment.CommentScreen
import com.example.kiendatn2.ui.post.CurrentPostState
import com.example.kiendatn2.ui.home.HomePage
import com.example.kiendatn2.ui.post.PostViewModel
import com.example.kiendatn2.ui.profile.ProfileScreen
import com.example.kiendatn2.ui.auth.AuthViewModel
import com.example.kiendatn2.ui.auth.LoginPage
import com.example.kiendatn2.ui.auth.SignupPage

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
        
        composable("profile") {
            ProfileScreen(
                postViewModel = postViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }
   })
}