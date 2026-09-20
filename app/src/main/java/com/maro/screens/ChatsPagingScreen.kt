package com.maro.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maro.R
import kotlinx.coroutines.launch

private data class DrawerMenuItem(
    val iconRes: Int,
    val title: String,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsPagingScreen(
    onNextButtonClickedToGo1: () -> Unit,
    onNextButtonClickedToGo2: () -> Unit,
    onNextButtonClickedToGo3: () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val menuItems = listOf(
        DrawerMenuItem(
            android.R.drawable.ic_menu_myplaces,
            stringResource(R.string.img1_text),
            onNextButtonClickedToGo1,
        ),
        DrawerMenuItem(
            android.R.drawable.ic_menu_manage,
            stringResource(R.string.img2_text),
            onNextButtonClickedToGo2,
        ),
        DrawerMenuItem(
            android.R.drawable.ic_menu_info_details,
            stringResource(R.string.img3_text),
            onNextButtonClickedToGo3,
        ),
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                items = menuItems,
                onItemClick = { item ->
                    scope.launch { drawerState.close() }
                    item.onClick()
                },
            )
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Maro") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_sort_by_size),
                                contentDescription = "Меню",
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            // Основное содержимое экрана
            Box(modifier = Modifier.padding(paddingValues)) {
                Text("Место в разработке")
            }
        }
    }
}

@Composable
private fun DrawerContent(
    items: List<DrawerMenuItem>,
    onItemClick: (DrawerMenuItem) -> Unit,
) {
    ModalDrawerSheet {
        Column(modifier = Modifier.fillMaxSize()) {
            // Заголовок меню
            Text(
                text = "Меню",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp),
            )

            HorizontalDivider()

            // Пункты меню
            items.forEach { item ->
                NavigationDrawerItem(
                    label = { Text(item.title) },
                    icon = {
                        Icon(
                            painter = painterResource(id = item.iconRes),
                            contentDescription = null,
                        )
                    },
                    selected = false,
                    onClick = { onItemClick(item) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }
    }
}
