package dev.sadakat.technonexttest.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.sadakat.technonexttest.presentation.ui.auth.AuthScreen
import dev.sadakat.technonexttest.presentation.ui.favorites.FavoritesScreen
import dev.sadakat.technonexttest.presentation.ui.posts.PostDetailScreen
import dev.sadakat.technonexttest.presentation.ui.posts.PostsScreen
import dev.sadakat.technonexttest.presentation.ui.profile.ProfileScreen
import dev.sadakat.technonexttest.presentation.ui.search.SearchScreen
import dev.sadakat.technonexttest.presentation.viewmodel.AuthViewModel
import dev.sadakat.technonexttest.presentation.viewmodel.PostsViewModel


sealed class Screen(val route: String, val title: String) {
    object Auth : Screen("auth", "Authentication")
    object Posts : Screen("posts", "Posts")
    object PostDetail : Screen("post_detail/{postId}", "Post Detail") {
        fun createRoute(postId: Int) = "post_detail/$postId"
    }

    object Search : Screen("search", "Search")
    object Favorites : Screen("favorites", "Favorites")
    object Profile : Screen("profile", "Profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    if (!authUiState.isLoggedIn) {
        AuthScreen(
            onAuthSuccess = {
                navController.navigate(Screen.Posts.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            })
    } else {
        Scaffold(
            bottomBar = {
                if (currentDestination?.route != Screen.PostDetail.route) {
                    NavigationBar {
                        bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                icon = {
                                Icon(
                                    screen.icon, contentDescription = screen.title
                                )
                            },
                                label = { Text(screen.title) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                })
                        }
                    }
                }
            }) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Posts.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Posts.route) {
                    PostsScreen(
                        onPostClick = { post ->
                            navController.navigate(Screen.PostDetail.createRoute(post.id))
                        })
                }

                composable(Screen.PostDetail.route) { backStackEntry ->
                    val postId = backStackEntry.arguments?.getString("postId")?.toIntOrNull() ?: 0
                    val postsViewModel: PostsViewModel = hiltViewModel()
                    val uiState by postsViewModel.uiState.collectAsStateWithLifecycle()

                    val post = uiState.posts.find { it.id == postId }

                    if (post != null) {
                        PostDetailScreen(
                            post = post,
                            onBackClick = { navController.popBackStack() },
                            onFavoriteClick = { postsViewModel.toggleFavorite(it) })
                    }
                }

                composable(Screen.Search.route) {
                    SearchScreen(
                        onPostClick = { post ->
                            navController.navigate(Screen.PostDetail.createRoute(post.id))
                        })
                }

                composable(Screen.Favorites.route) {
                    FavoritesScreen(
                        onPostClick = { post ->
                            navController.navigate(Screen.PostDetail.createRoute(post.id))
                        })
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(authViewModel)
                }
            }
        }
    }
}

data class BottomNavItem(
    val route: String, val title: String, val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Posts.route, "Posts", Icons.Filled.Home),
    BottomNavItem(Screen.Search.route, "Search", Icons.Filled.Search),
    BottomNavItem(Screen.Favorites.route, "Favorites", Icons.Filled.Favorite),
    BottomNavItem(Screen.Profile.route, "Profile", Icons.Filled.Person)
)

