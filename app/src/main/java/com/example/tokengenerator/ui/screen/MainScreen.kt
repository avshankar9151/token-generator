package com.example.tokengenerator.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.tokengenerator.constant.Constant.GENERATE_TOKEN
import com.example.tokengenerator.constant.Constant.MODIFY_REFERENCE_PERSONS
import com.example.tokengenerator.constant.Constant.VIEW_REPORT
import com.example.tokengenerator.dataClass.NavItem


val navItems = listOf(
    NavItem(title = "Token", icon = Icons.Default.Add, route = GENERATE_TOKEN),
    NavItem(title = "Persons", icon = Icons.Default.Person, route = MODIFY_REFERENCE_PERSONS),
    NavItem(title = "Report", icon = Icons.Default.Menu, route = VIEW_REPORT),
)

@Composable
fun MainScreen(modifier: Modifier = Modifier) {

    var selectedNavIndex by remember { mutableIntStateOf(0) }

    Scaffold (
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = index == selectedNavIndex,
                        onClick = {
                            selectedNavIndex = index
                        },
                        label = { Text(item.title) },
                        icon = { Icon(item.icon, contentDescription = item.title) }
                    )
                }
            }
        }
    ) {
        innerPadding -> Box(modifier = modifier.padding(innerPadding)) { ContentScreen(selectedNavIndex = selectedNavIndex)}
    }
}

@Composable
fun ContentScreen(selectedNavIndex: Int) {
        when (navItems[selectedNavIndex].route) {
            GENERATE_TOKEN -> {
                GenerateTokenScreen()
            }
            MODIFY_REFERENCE_PERSONS -> {
                AddOrEditPersonScreen()
            }
            VIEW_REPORT -> {
                ViewReportScreen()
            }
        }
}