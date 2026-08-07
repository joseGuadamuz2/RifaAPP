package com.example.lotteryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lotteryapp.ui.home.HomeScreen
import com.example.lotteryapp.ui.home.HomeViewModel
import com.example.lotteryapp.ui.raffle.CreateRaffleScreen
import com.example.lotteryapp.ui.raffle.CreateRaffleViewModel
import com.example.lotteryapp.ui.raffle.EditRaffleScreen
import com.example.lotteryapp.ui.raffle.EditRaffleViewModel
import com.example.lotteryapp.ui.raffle.SoldTicketsScreen
import com.example.lotteryapp.ui.raffle.SoldTicketsViewModel
import com.example.lotteryapp.ui.raffle.TicketGridScreen
import com.example.lotteryapp.ui.raffle.TicketGridViewModel
import com.example.lotteryapp.ui.theme.LotteryAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LotteryAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val repository = (application as LotteryApp).repository
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            val viewModel: HomeViewModel = viewModel(
                                factory = HomeViewModel.Factory(repository)
                            )
                            HomeScreen(
                                viewModel = viewModel,
                                onCreateRaffle = { navController.navigate("createRaffle") },
                                onOpenRaffle = { raffleId -> navController.navigate("ticketGrid/$raffleId") },
                                onEditRaffle = { raffleId -> navController.navigate("editRaffle/$raffleId") }
                            )
                        }

                        composable("createRaffle") {
                            val viewModel: CreateRaffleViewModel = viewModel(
                                factory = CreateRaffleViewModel.Factory(repository)
                            )
                            CreateRaffleScreen(
                                viewModel = viewModel,
                                onRaffleSaved = { raffleId ->
                                    navController.navigate("ticketGrid/$raffleId") {
                                        popUpTo("home")
                                    }
                                }
                            )
                        }

                        composable(
                            route = "editRaffle/{raffleId}",
                            arguments = listOf(navArgument("raffleId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val raffleId = backStackEntry.arguments?.getString("raffleId") ?: return@composable
                            val viewModel: EditRaffleViewModel = viewModel(
                                factory = EditRaffleViewModel.Factory(repository, raffleId)
                            )
                            EditRaffleScreen(
                                viewModel = viewModel,
                                onRaffleUpdated = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "ticketGrid/{raffleId}",
                            arguments = listOf(navArgument("raffleId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val raffleId = backStackEntry.arguments?.getString("raffleId") ?: return@composable
                            val viewModel: TicketGridViewModel = viewModel(
                                factory = TicketGridViewModel.Factory(repository, raffleId)
                            )
                            TicketGridScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onOpenSoldTickets = { navController.navigate("soldTickets/$raffleId") }
                            )
                        }

                        composable(
                            route = "soldTickets/{raffleId}",
                            arguments = listOf(navArgument("raffleId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val raffleId = backStackEntry.arguments?.getString("raffleId") ?: return@composable
                            val viewModel: SoldTicketsViewModel = viewModel(
                                factory = SoldTicketsViewModel.Factory(repository, raffleId)
                            )
                            SoldTicketsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}