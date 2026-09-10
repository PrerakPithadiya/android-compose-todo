package com.example.todo_list.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.data.repository.WorldTimezoneRepository
import com.example.todo_list.manager.TimePreferencesManager
import com.example.todo_list.model.WorldLocation
import com.example.todo_list.ui.theme.*
import com.example.todo_list.utils.HapticManager
import com.example.todo_list.utils.TimeFormatHelper

/**
 * Authentic Apple iOS Modal Bottom Sheet for Searching & Selecting World Cities and Countries.
 * Supports auto-suggestions for any world city or country, displaying live digital clocks,
 * flags, and standard time zone offsets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimezonePickerBottomSheet(
    onDismiss: () -> Unit,
    onLocationSelected: (WorldLocation) -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val is24Hour = TimePreferencesManager.is24HourFormat
    val currentLocation = TimePreferencesManager.selectedLocation

    val searchResults = remember(searchQuery) {
        WorldTimezoneRepository.search(searchQuery)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SystemSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet Header with Title & Close Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Time Zone & Region",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SystemLabelPrimary
                    )
                    Text(
                        text = "Select country or city across the world",
                        fontSize = 13.sp,
                        color = SystemLabelSecondary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .background(SystemGray5, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = SystemLabelPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Current Active Timezone Glass Card with Live Clock
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SystemGroupedBackground,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = currentLocation.flagEmoji, fontSize = 28.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${currentLocation.cityName}, ${currentLocation.countryName}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SystemLabelPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SystemBlueLight)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SystemBlue
                                    )
                                }
                            }
                            Text(
                                text = "${currentLocation.timeZoneName} • ${currentLocation.utcOffsetStr}",
                                fontSize = 12.sp,
                                color = SystemLabelSecondary
                            )
                        }
                    }

                    // Live digital clock preview
                    Text(
                        text = TimePreferencesManager.getLiveLocalTime(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = SystemBlue
                    )
                }
            }

            // iOS Styled Search Input Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SystemGray5,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = SystemLabelSecondary,
                        modifier = Modifier.size(18.dp)
                    )

                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search country, city, or standard time...",
                                fontSize = 14.sp,
                                color = SystemLabelSecondary
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear",
                                tint = SystemLabelSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Search Results & Suggestions List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Section 1: Matching Country (if query matches a Country name)
                if (searchResults.countryMatches.isNotEmpty()) {
                    item(key = "header_countries") {
                        Text(
                            text = "COUNTRIES & STANDARD TIME",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SystemLabelSecondary,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                        )
                    }

                    items(searchResults.countryMatches, key = { it.id }) { country ->
                        val isSelected = currentLocation.countryCode.equals(country.countryCode, ignoreCase = true) &&
                                currentLocation.timeZoneId == country.timeZoneId

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) SystemBlueLight else SystemGroupedBackground,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, SystemBlue) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    HapticManager.performClick(context)
                                    TimePreferencesManager.selectLocation(country)
                                    onLocationSelected(country)
                                    Toast.makeText(context, "Region set to ${country.countryName}", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(text = country.flagEmoji, fontSize = 24.sp)
                                    Column {
                                        Text(
                                            text = country.countryName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) SystemBlue else SystemLabelPrimary
                                        )
                                        Text(
                                            text = "Standard Time: ${country.timeZoneAbbr} (${country.utcOffsetStr})",
                                            fontSize = 12.sp,
                                            color = SystemLabelSecondary
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) SystemBlue else SystemSurface,
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider)
                                ) {
                                    Text(
                                        text = if (isSelected) "Selected" else "Set Country",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else SystemBlue,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: Matching Cities & Timezones
                if (searchResults.cityMatches.isNotEmpty()) {
                    item(key = "header_cities") {
                        Text(
                            text = "WORLD CITIES & TIME ZONES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SystemLabelSecondary,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
                        )
                    }

                    items(searchResults.cityMatches, key = { it.id }) { city ->
                        val isSelected = currentLocation.cityName.equals(city.cityName, ignoreCase = true) &&
                                currentLocation.timeZoneId == city.timeZoneId

                        val cityLiveTime = TimeFormatHelper.getFormattedCurrentTime(city.timeZoneId, is24Hour)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SystemBlueLight else SystemGroupedBackground,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, SystemBlue) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    HapticManager.performClick(context)
                                    TimePreferencesManager.selectLocation(city)
                                    onLocationSelected(city)
                                    Toast.makeText(context, "Time zone switched to ${city.cityName}, ${city.countryName}", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = city.flagEmoji, fontSize = 20.sp)
                                    Column {
                                        Text(
                                            text = "${city.cityName}, ${city.countryName}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSelected) SystemBlue else SystemLabelPrimary
                                        )
                                        Text(
                                            text = "${city.timeZoneAbbr} • ${city.utcOffsetStr}",
                                            fontSize = 12.sp,
                                            color = SystemLabelSecondary
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = cityLiveTime,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) SystemBlue else SystemLabelSecondary
                                    )

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = SystemBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (searchResults.countryMatches.isEmpty()) {
                    item(key = "empty_state") {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Public,
                                    contentDescription = null,
                                    tint = SystemLabelSecondary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No matching city or country found",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SystemLabelPrimary
                                )
                                Text(
                                    text = "Try typing a country name, city, or UTC offset",
                                    fontSize = 13.sp,
                                    color = SystemLabelSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
