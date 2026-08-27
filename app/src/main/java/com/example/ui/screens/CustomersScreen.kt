package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.Customer
import com.example.data.models.CustomerTransaction
import com.example.data.models.Sale
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodels.AntarSalesViewModel
import com.example.utils.AppLanguage
import com.example.utils.AppStrings
import java.text.SimpleDateFormat
import java.util.*

enum class CustomerFilter {
    ALL,
    FAVORITES,
    WITH_DEBT,
    NO_DEBT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: AntarSalesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLang by viewModel.currentLanguage.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val favoriteCustomers by viewModel.favoriteCustomers.collectAsState()
    val customersWithDebt by viewModel.customersWithDebt.collectAsState()
    val totalDebt by viewModel.totalCustomerDebt.collectAsState()
    val selectedPosCustomer by viewModel.selectedPosCustomer.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CustomerFilter.ALL) }

    // Dialogs state
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerForPayment by remember { mutableStateOf<Customer?>(null) }
    var customerForAddDebt by remember { mutableStateOf<Customer?>(null) }
    var customerForStatement by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }

    // CSV Launchers
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportCustomersToCsv(context, it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importCustomersFromCsv(context, it) }
    }

    val filteredCustomers = remember(customers, searchQuery, selectedFilter) {
        customers.filter { cust ->
            val matchesQuery = if (searchQuery.isBlank()) true else {
                cust.name.contains(searchQuery, ignoreCase = true) ||
                        cust.phone.contains(searchQuery, ignoreCase = true) ||
                        cust.email.contains(searchQuery, ignoreCase = true) ||
                        cust.address.contains(searchQuery, ignoreCase = true) ||
                        cust.notes.contains(searchQuery, ignoreCase = true)
            }
            val matchesFilter = when (selectedFilter) {
                CustomerFilter.ALL -> true
                CustomerFilter.FAVORITES -> cust.isFavorite
                CustomerFilter.WITH_DEBT -> cust.balanceDebt > 0
                CustomerFilter.NO_DEBT -> cust.balanceDebt <= 0
            }
            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddCustomerDialog = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text(AppStrings.addCustomer(currentLang), fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_customer_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ===== Customer Directory Header Banner =====
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Contacts,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = AppStrings.customerDirectoryTitle(currentLang),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = AppStrings.customerDirectoryDesc(currentLang),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (selectedPosCustomer != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { viewModel.selectPosCustomer(null) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Text(
                                    text = selectedPosCustomer!!.name,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Icon(Icons.Default.Close, contentDescription = "Deselect", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ===== Top KPI Cards Row =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: Total Debt
                Card(
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (totalDebt > 0) DangerRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = if (totalDebt > 0) DangerRed else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (currentLang == AppLanguage.FRENCH) "Total dettes" else if (currentLang == AppLanguage.ENGLISH) "Total Debt" else "إجمالي الديون",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%,.2f %s", totalDebt, AppStrings.currency(currentLang)),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalDebt > 0) DangerRed else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Card 2: Debtors Count
                Card(
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (currentLang == AppLanguage.FRENCH) "Débiteurs" else if (currentLang == AppLanguage.ENGLISH) "Debtors" else "زبائن مدينون",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (currentLang == AppLanguage.FRENCH) "${customersWithDebt.size} clients" else if (currentLang == AppLanguage.ENGLISH) "${customersWithDebt.size} debtors" else "${customersWithDebt.size} زبون",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (customersWithDebt.isNotEmpty()) WarningOrange else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Card 3: Favorites Count
                Card(
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (currentLang == AppLanguage.FRENCH) "Favoris ⭐" else if (currentLang == AppLanguage.ENGLISH) "Favorites ⭐" else "المميزون ⭐",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${favoriteCustomers.size}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ===== Search & Quick CSV Actions =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = AppStrings.searchCustomerHint(currentLang),
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_customer_field"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                FilledTonalIconButton(
                    onClick = { exportLauncher.launch("antar_customers_${System.currentTimeMillis()}.csv") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export CSV")
                }

                FilledTonalIconButton(
                    onClick = { importLauncher.launch("text/*") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Import CSV")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ===== Filter Chips =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == CustomerFilter.ALL,
                    onClick = { selectedFilter = CustomerFilter.ALL },
                    label = { Text(if (currentLang == AppLanguage.FRENCH) "Tous (${customers.size})" else if (currentLang == AppLanguage.ENGLISH) "All (${customers.size})" else "الكل (${customers.size})", fontSize = 11.sp) },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = selectedFilter == CustomerFilter.FAVORITES,
                    onClick = { selectedFilter = CustomerFilter.FAVORITES },
                    label = { Text(if (currentLang == AppLanguage.FRENCH) "⭐ Favoris (${favoriteCustomers.size})" else if (currentLang == AppLanguage.ENGLISH) "⭐ Star (${favoriteCustomers.size})" else "⭐ المميزون (${favoriteCustomers.size})", fontSize = 11.sp) },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = selectedFilter == CustomerFilter.WITH_DEBT,
                    onClick = { selectedFilter = CustomerFilter.WITH_DEBT },
                    label = { Text(if (currentLang == AppLanguage.FRENCH) "Dettes (${customersWithDebt.size})" else if (currentLang == AppLanguage.ENGLISH) "Debt (${customersWithDebt.size})" else "ديون (${customersWithDebt.size})", fontSize = 11.sp) },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = selectedFilter == CustomerFilter.NO_DEBT,
                    onClick = { selectedFilter = CustomerFilter.NO_DEBT },
                    label = { Text(if (currentLang == AppLanguage.FRENCH) "Réglés" else if (currentLang == AppLanguage.ENGLISH) "Clear" else "خالص", fontSize = 11.sp) },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ===== Customers List =====
            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PersonSearch,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                if (currentLang == AppLanguage.FRENCH) "Aucun client ne correspond à la recherche" else if (currentLang == AppLanguage.ENGLISH) "No matching customers found" else "لا توجد نتائج مطابقة للبحث"
                            } else {
                                if (currentLang == AppLanguage.FRENCH) "Aucun client dans cette catégorie" else if (currentLang == AppLanguage.ENGLISH) "No customers in this category" else "لم يتم تسجيل أي زبائن بعد"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (currentLang == AppLanguage.FRENCH) "Appuyez sur le bouton + ci-dessous pour ajouter un client" else if (currentLang == AppLanguage.ENGLISH) "Tap the + button below to add a new customer" else "اضغط على زر الإضافة بالأسفل لإدخال زبون جديد",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("customers_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        val isSelectedForPos = selectedPosCustomer?.id == customer.id
                        CustomerItemCard(
                            customer = customer,
                            currentLang = currentLang,
                            isSelectedForPos = isSelectedForPos,
                            onToggleFavorite = { viewModel.toggleCustomerFavorite(customer) },
                            onSelectForFastCheckout = {
                                if (isSelectedForPos) {
                                    viewModel.selectPosCustomer(null)
                                } else {
                                    viewModel.selectPosCustomer(customer)
                                    viewModel.showMessage("⚡ تم اختيار الزبون ${customer.name} للبيع السريع")
                                }
                            },
                            onRecordPayment = { customerForPayment = customer },
                            onAddDebt = { customerForAddDebt = customer },
                            onViewStatement = { customerForStatement = customer },
                            onEdit = { editingCustomer = customer },
                            onDelete = { customerToDelete = customer },
                            onCallPhone = { phone ->
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            onSendWhatsApp = { phone ->
                                try {
                                    val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    try {
                                        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone"))
                                        context.startActivity(smsIntent)
                                    } catch (_: Exception) {}
                                }
                            },
                            onSendEmail = { emailAddr ->
                                try {
                                    val mailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$emailAddr"))
                                    context.startActivity(mailIntent)
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }
        }
    }

    // ===== Add / Edit Customer Dialog =====
    if (showAddCustomerDialog || editingCustomer != null) {
        val isEditing = editingCustomer != null
        var nameInput by remember { mutableStateOf(editingCustomer?.name ?: "") }
        var phoneInput by remember { mutableStateOf(editingCustomer?.phone ?: "") }
        var emailInput by remember { mutableStateOf(editingCustomer?.email ?: "") }
        var addressInput by remember { mutableStateOf(editingCustomer?.address ?: "") }
        var notesInput by remember { mutableStateOf(editingCustomer?.notes ?: "") }
        var isFavoriteInput by remember { mutableStateOf(editingCustomer?.isFavorite ?: false) }
        var initialDebtInput by remember { mutableStateOf(if (isEditing) editingCustomer!!.balanceDebt.toString() else "0") }

        AlertDialog(
            onDismissRequest = {
                showAddCustomerDialog = false
                editingCustomer = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditing) {
                            if (currentLang == AppLanguage.FRENCH) "Modifier le client" else if (currentLang == AppLanguage.ENGLISH) "Edit Customer" else "تعديل بيانات الزبون"
                        } else {
                            AppStrings.addCustomer(currentLang)
                        }
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("${AppStrings.customerName(currentLang)} *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("customer_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text(AppStrings.phone(currentLang)) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("customer_phone_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text(AppStrings.email(currentLang)) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth().testTag("customer_email_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text(AppStrings.address(currentLang)) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("customer_address_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text(AppStrings.notes(currentLang)) },
                        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("customer_notes_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Favorite Checkbox Row
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isFavoriteInput = !isFavoriteInput }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = if (isFavoriteInput) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = if (isFavoriteInput) WarningOrange else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = AppStrings.favoriteCustomer(currentLang),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Switch(
                                checked = isFavoriteInput,
                                onCheckedChange = { isFavoriteInput = it }
                            )
                        }
                    }

                    if (!isEditing) {
                        OutlinedTextField(
                            value = initialDebtInput,
                            onValueChange = { initialDebtInput = it },
                            label = { Text(if (currentLang == AppLanguage.FRENCH) "Solde débiteur initial (optionnel)" else if (currentLang == AppLanguage.ENGLISH) "Initial Debt (optional)" else "رصيد مديونية افتتاحي سابق (اختياري)") },
                            leadingIcon = { Icon(Icons.Default.MoneyOff, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("customer_initial_debt_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            val initDebt = initialDebtInput.toDoubleOrNull() ?: 0.0
                            if (isEditing) {
                                val updated = editingCustomer!!.copy(
                                    name = nameInput.trim(),
                                    phone = phoneInput.trim(),
                                    email = emailInput.trim(),
                                    address = addressInput.trim(),
                                    notes = notesInput.trim(),
                                    isFavorite = isFavoriteInput
                                )
                                viewModel.updateCustomer(updated)
                            } else {
                                viewModel.addCustomer(
                                    name = nameInput.trim(),
                                    phone = phoneInput.trim(),
                                    email = emailInput.trim(),
                                    address = addressInput.trim(),
                                    notes = notesInput.trim(),
                                    isFavorite = isFavoriteInput,
                                    initialDebt = initDebt
                                )
                            }
                            showAddCustomerDialog = false
                            editingCustomer = null
                        }
                    },
                    modifier = Modifier.testTag("save_customer_button"),
                    enabled = nameInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isEditing) AppStrings.save(currentLang) else AppStrings.addCustomer(currentLang))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddCustomerDialog = false
                    editingCustomer = null
                }) {
                    Text(AppStrings.cancel(currentLang))
                }
            }
        )
    }

    // ===== Record Payment Dialog =====
    customerForPayment?.let { customer ->
        var paymentAmountText by remember { mutableStateOf("") }
        var paymentNotesText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { customerForPayment = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payment, contentDescription = null, tint = SuccessGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStrings.recordPayment(currentLang))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${AppStrings.customerName(currentLang)}: ${customer.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DangerRed.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (currentLang == AppLanguage.FRENCH) "Dette actuelle :" else if (currentLang == AppLanguage.ENGLISH) "Current Debt:" else "الرصيد المدين الحالي:", fontSize = 12.sp)
                            Text(
                                String.format(Locale.getDefault(), "%.2f %s", customer.balanceDebt, AppStrings.currency(currentLang)),
                                fontWeight = FontWeight.Bold,
                                color = DangerRed
                            )
                        }
                    }

                    OutlinedTextField(
                        value = paymentAmountText,
                        onValueChange = { paymentAmountText = it },
                        label = { Text("${if (currentLang == AppLanguage.FRENCH) "Montant reçu" else if (currentLang == AppLanguage.ENGLISH) "Paid Amount" else "المبلغ المسدد"} (${AppStrings.currency(currentLang)}) *") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("payment_amount_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = paymentNotesText,
                        onValueChange = { paymentNotesText = it },
                        label = { Text(if (currentLang == AppLanguage.FRENCH) "Remarques (optionnel)" else if (currentLang == AppLanguage.ENGLISH) "Notes (optional)" else "ملاحظات السداد (اختياري)") },
                        leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentAmountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.recordCustomerPayment(
                                customerId = customer.id,
                                customerName = customer.name,
                                amount = amount,
                                notes = paymentNotesText
                            )
                            customerForPayment = null
                        }
                    },
                    modifier = Modifier.testTag("confirm_payment_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text(if (currentLang == AppLanguage.FRENCH) "Confirmer paiement 💵" else if (currentLang == AppLanguage.ENGLISH) "Confirm Payment 💵" else "تأكيد السداد 💵")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerForPayment = null }) {
                    Text(AppStrings.cancel(currentLang))
                }
            }
        )
    }

    // ===== Add Manual Debt Dialog =====
    customerForAddDebt?.let { customer ->
        var debtAmountText by remember { mutableStateOf("") }
        var debtNotesText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { customerForAddDebt = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, tint = WarningOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStrings.addManualDebt(currentLang))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${AppStrings.customerName(currentLang)}: ${customer.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    OutlinedTextField(
                        value = debtAmountText,
                        onValueChange = { debtAmountText = it },
                        label = { Text("${if (currentLang == AppLanguage.FRENCH) "Montant de la dette" else if (currentLang == AppLanguage.ENGLISH) "Debt Amount" else "مبلغ الدين المراد إضافته"} (${AppStrings.currency(currentLang)}) *") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("add_debt_amount_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = debtNotesText,
                        onValueChange = { debtNotesText = it },
                        label = { Text(if (currentLang == AppLanguage.FRENCH) "Détails / Motif" else if (currentLang == AppLanguage.ENGLISH) "Reason / Details" else "سبب الدين / تفاصيل الطلبية") },
                        leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = debtAmountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.addCustomerDebt(
                                customerId = customer.id,
                                customerName = customer.name,
                                amount = amount,
                                notes = debtNotesText
                            )
                            customerForAddDebt = null
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (currentLang == AppLanguage.FRENCH) "Ajouter dette ➕" else if (currentLang == AppLanguage.ENGLISH) "Add Debt ➕" else "إضافة الدين ➕")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerForAddDebt = null }) {
                    Text(AppStrings.cancel(currentLang))
                }
            }
        )
    }

    // ===== Customer Account Statement Dialog =====
    customerForStatement?.let { customer ->
        CustomerStatementDialog(
            customer = customer,
            viewModel = viewModel,
            currentLang = currentLang,
            onDismiss = { customerForStatement = null }
        )
    }

    // ===== Delete Customer Confirmation Dialog =====
    customerToDelete?.let { customer ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (currentLang == AppLanguage.FRENCH) "Supprimer le client" else if (currentLang == AppLanguage.ENGLISH) "Delete Customer" else "تأكيد حذف الزبون")
                }
            },
            text = {
                Text(
                    if (currentLang == AppLanguage.FRENCH) "Êtes-vous sûr de vouloir supprimer le client \"${customer.name}\" et toutes ses transactions ? Cette action est irréversible."
                    else if (currentLang == AppLanguage.ENGLISH) "Are you sure you want to delete customer \"${customer.name}\" and all transaction records? This action cannot be undone."
                    else "هل أنت متأكد من حذف الزبون \"${customer.name}\" وجميع سجلات مدفوعاته ومعاملاته؟ لا يمكن التراجع عن هذا الإجراء."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(customer.id)
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (currentLang == AppLanguage.FRENCH) "Oui, supprimer" else if (currentLang == AppLanguage.ENGLISH) "Yes, Delete" else "نعم، حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text(AppStrings.cancel(currentLang))
                }
            }
        )
    }
}

@Composable
fun CustomerItemCard(
    customer: Customer,
    currentLang: AppLanguage = AppLanguage.ARABIC,
    isSelectedForPos: Boolean = false,
    onToggleFavorite: () -> Unit,
    onSelectForFastCheckout: () -> Unit,
    onRecordPayment: () -> Unit,
    onAddDebt: () -> Unit,
    onViewStatement: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCallPhone: (String) -> Unit,
    onSendWhatsApp: (String) -> Unit,
    onSendEmail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasDebt = customer.balanceDebt > 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("customer_card_${customer.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelectedForPos) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else if (hasDebt) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isSelectedForPos) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (hasDebt || isSelectedForPos) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Avatar, Name, Favorite Icon & Debt Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Initial Letter Avatar with Favorite Badge
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = if (hasDebt) DangerRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = customer.name.firstOrNull()?.toString() ?: "ز",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = if (hasDebt) DangerRed else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (customer.isFavorite) {
                            Surface(
                                shape = CircleShape,
                                color = WarningOrange,
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.BottomEnd)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Favorite",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = customer.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (customer.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Toggle Favorite",
                                    tint = if (customer.isFavorite) WarningOrange else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (customer.phone.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { onCallPhone(customer.phone) }
                                    .padding(top = 1.dp)
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = customer.phone,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (customer.email.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { onSendEmail(customer.email) }
                                    .padding(top = 1.dp)
                            ) {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = customer.email,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Balance Debt Badge & Fast Checkout Toggle
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasDebt) DangerRed.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = if (hasDebt) (if (currentLang == AppLanguage.FRENCH) "Dette" else if (currentLang == AppLanguage.ENGLISH) "Debt" else "مديونية")
                                else (if (currentLang == AppLanguage.FRENCH) "Réglé" else if (currentLang == AppLanguage.ENGLISH) "Clear" else "خالص"),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasDebt) DangerRed else SuccessGreen
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f %s", customer.balanceDebt, AppStrings.currency(currentLang)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (hasDebt) DangerRed else SuccessGreen
                            )
                        }
                    }

                    // Fast Checkout selection button
                    FilledTonalButton(
                        onClick = onSelectForFastCheckout,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp),
                        colors = if (isSelectedForPos) ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
                        else ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = if (isSelectedForPos) Icons.Default.Check else Icons.Default.FlashOn,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isSelectedForPos) (if (currentLang == AppLanguage.FRENCH) "Sélectionné" else if (currentLang == AppLanguage.ENGLISH) "Selected" else "محدد للبيع")
                            else (if (currentLang == AppLanguage.FRENCH) "Vente rapide" else if (currentLang == AppLanguage.ENGLISH) "Fast POS" else "بيع سريع ⚡"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Address and Notes Row if available
            if (customer.address.isNotBlank() || customer.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (customer.address.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = customer.address,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (customer.notes.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = customer.notes,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Bar: Payment, Debt, Statement, Call/WhatsApp, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    // Payment Button
                    FilledTonalButton(
                        onClick = onRecordPayment,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SuccessGreen.copy(alpha = 0.15f),
                            contentColor = SuccessGreen
                        )
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (currentLang == AppLanguage.FRENCH) "Paiement" else if (currentLang == AppLanguage.ENGLISH) "Pay" else "سداد 💵", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Add Debt Button
                    FilledTonalButton(
                        onClick = onAddDebt,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (currentLang == AppLanguage.FRENCH) "Dette" else if (currentLang == AppLanguage.ENGLISH) "Debt" else "دين ➕", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Statement Button
                    FilledTonalButton(
                        onClick = onViewStatement,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (currentLang == AppLanguage.FRENCH) "Relevé" else if (currentLang == AppLanguage.ENGLISH) "Statement" else "كشف", fontSize = 11.sp)
                    }
                }

                // Contact, Edit & Delete
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (customer.phone.isNotBlank()) {
                        IconButton(
                            onClick = { onSendWhatsApp(customer.phone) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = "WhatsApp / SMS",
                                modifier = Modifier.size(16.dp),
                                tint = SuccessGreen
                            )
                        }
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "تعديل",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "حذف",
                            modifier = Modifier.size(16.dp),
                            tint = DangerRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerStatementDialog(
    customer: Customer,
    viewModel: AntarSalesViewModel,
    currentLang: AppLanguage = AppLanguage.ARABIC,
    onDismiss: () -> Unit
) {
    val transactionsFlow = remember(customer.id) { viewModel.getCustomerTransactions(customer.id) }
    val transactions by transactionsFlow.collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 12.dp)
                .testTag("customer_statement_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "${AppStrings.statement(currentLang)}: ${customer.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (customer.phone.isNotBlank()) {
                                Text(
                                    text = customer.phone,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary Financials
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (currentLang == AppLanguage.FRENCH) "Solde débiteur restant" else if (currentLang == AppLanguage.ENGLISH) "Remaining Debt" else "الرصيد المدين المتبقي",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f %s", customer.balanceDebt, AppStrings.currency(currentLang)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (customer.balanceDebt > 0) DangerRed else SuccessGreen
                            )
                        }
                        VerticalDivider(modifier = Modifier.height(32.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                AppStrings.totalPurchases(currentLang),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f %s", customer.totalPurchases, AppStrings.currency(currentLang)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (currentLang == AppLanguage.FRENCH) "Historique des transactions (${transactions.size}) :" else if (currentLang == AppLanguage.ENGLISH) "Transaction History (${transactions.size}):" else "سجل الحركات (${transactions.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentLang == AppLanguage.FRENCH) "Aucune transaction enregistrée pour ce client" else if (currentLang == AppLanguage.ENGLISH) "No transactions recorded for this customer yet" else "لا توجد حركات مسجلة لهذا الزبون حتى الآن",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(transactions, key = { it.id }) { tx ->
                            val isPayment = tx.type == "PAYMENT"
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPayment) SuccessGreen.copy(alpha = 0.08f) else DangerRed.copy(alpha = 0.08f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isPayment) SuccessGreen.copy(alpha = 0.2f) else DangerRed.copy(alpha = 0.2f),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (isPayment) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                    contentDescription = null,
                                                    tint = if (isPayment) SuccessGreen else DangerRed,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = if (isPayment) {
                                                    if (currentLang == AppLanguage.FRENCH) "Règlement paiement" else if (currentLang == AppLanguage.ENGLISH) "Payment Received" else "سداد دفعة نقدية"
                                                } else {
                                                    if (currentLang == AppLanguage.FRENCH) "Facture / Dette à crédit" else if (currentLang == AppLanguage.ENGLISH) "Invoice / Debt" else "فاتورة / دين آجل"
                                                },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isPayment) SuccessGreen else DangerRed
                                            )
                                            if (tx.notes.isNotBlank()) {
                                                Text(
                                                    text = tx.notes,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Text(
                                                text = dateFormat.format(Date(tx.date)),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        text = String.format(Locale.getDefault(), "%s%.2f %s", if (isPayment) "-" else "+", tx.amount, AppStrings.currency(currentLang)),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = if (isPayment) SuccessGreen else DangerRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
