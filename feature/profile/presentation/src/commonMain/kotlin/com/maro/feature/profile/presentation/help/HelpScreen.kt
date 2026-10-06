package com.maro.feature.profile.presentation.help

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maro.feature.profile.presentation.generated.resources.Res
import com.maro.feature.profile.presentation.generated.resources.help_back
import com.maro.feature.profile.presentation.generated.resources.help_contact_call
import com.maro.feature.profile.presentation.generated.resources.help_contact_email
import com.maro.feature.profile.presentation.generated.resources.help_contact_site
import com.maro.feature.profile.presentation.generated.resources.help_contacts
import com.maro.feature.profile.presentation.generated.resources.help_faq
import com.maro.feature.profile.presentation.generated.resources.help_faq_change_phone
import com.maro.feature.profile.presentation.generated.resources.help_faq_create_group
import com.maro.feature.profile.presentation.generated.resources.help_faq_hide_last_seen
import com.maro.feature.profile.presentation.generated.resources.help_features
import com.maro.feature.profile.presentation.generated.resources.help_features_text
import com.maro.feature.profile.presentation.generated.resources.help_heading
import com.maro.feature.profile.presentation.generated.resources.help_subheading
import com.maro.feature.profile.presentation.generated.resources.help_support_chat
import com.maro.feature.profile.presentation.generated.resources.help_support_chat_text
import com.maro.feature.profile.presentation.generated.resources.help_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit,
    onChangedPhoneClick: () -> Unit,
    onHideLastSessionClick: () -> Unit,
    onCreateGroupChatClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.help_title)) },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.help_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            // Заголовок
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Help,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.help_heading),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.help_subheading),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Основные разделы
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    HelpItem(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        title = stringResource(Res.string.help_support_chat),
                        description = stringResource(Res.string.help_support_chat_text),
                        onClick = { /*navController.navigate("support_chat")*/ },
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    )

                    HelpItem(
                        icon = Icons.Default.Info,
                        title = stringResource(Res.string.help_features),
                        description = stringResource(Res.string.help_features_text),
                        onClick = { /* navController.navigate("features")*/ },
                    )
                }
            }

            // FAQ раздел
            item {
                Text(
                    text = stringResource(Res.string.help_faq),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp),
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    listOf(
                        stringResource(Res.string.help_faq_change_phone),
                        stringResource(Res.string.help_faq_hide_last_seen),
                        stringResource(Res.string.help_faq_create_group),
                    ).forEachIndexed { index, question ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            )
                        }
                        FAQItem(
                            question = question,
                            onClick = {
                                when (index) {
                                    0 -> onChangedPhoneClick()
                                    1 -> onHideLastSessionClick()
                                    2 -> onCreateGroupChatClick()
                                }
                            },
                        )
                    }
                }
            }

            // Контакты
            item {
                Text(
                    text = stringResource(Res.string.help_contacts),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ContactOption(
                        icon = Icons.Default.Email,
                        label = stringResource(Res.string.help_contact_email),
                        onClick = { /* Открыть почту */ },
                    )
                    ContactOption(
                        icon = Icons.Default.Web,
                        label = stringResource(Res.string.help_contact_site),
                        onClick = { /* Открыть сайт */ },
                    )
                    ContactOption(
                        icon = Icons.Default.Phone,
                        label = stringResource(Res.string.help_contact_call),
                        onClick = { /* Позвонить */ },
                    )
                }
            }
        }
    }
}

@Composable
fun HelpItem(icon: ImageVector, title: String, description: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 14.sp,
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
fun FAQItem(question: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = question,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
fun ContactOption(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
    }
}
