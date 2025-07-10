package com.example.example.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.example.R
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChatsScreen() {
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    var selectedItem by remember { mutableStateOf(0) }

    val menuItems2 = mutableListOf<Pair<Int, String>>(
        Pair(android.R.drawable.ic_menu_myplaces, stringResource(R.string.img1_text)),
        Pair(android.R.drawable.ic_menu_manage, stringResource(R.string.img2_text)),
        Pair(android.R.drawable.ic_menu_info_details, stringResource(R.string.img3_text)))

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text("Maro") },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch { scaffoldState.drawerState.open() }
                    }) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_sort_by_size), // Ваша иконка меню
                            contentDescription = "Меню"
                        )
                    }
                }
            )
        },
        drawerContent = {
            DrawerContent(
                items = menuItems2,
                selectedItem = selectedItem,
                onItemClick = { index ->
                    selectedItem = index
                    scope.launch { scaffoldState.drawerState.close() }
                }
            )
        }
    ) { paddingValues ->
        // Основное содержимое экрана
        Box(modifier = Modifier.padding(paddingValues)) {
            Text("Место в разработке")
        }
    }
}

@ExperimentalMaterialApi
@Composable
fun DrawerContent(
    items: List<Pair<Int, String>>,
    selectedItem: Int,
    onItemClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Заголовок меню
        Text(
            text = "Меню",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(16.dp)
        )

        Divider()

        // Пункты меню
        items.forEachIndexed { index, item ->
            ListItem(
                text = { Text(item.second) },
                icon = { GlideImage(item.first, modifier = Modifier.size(35.dp)) },

            )
        }
    }
}