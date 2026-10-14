package com.pennywise.money

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private val Purple = Color(0xFF087F5B)
private val Pale = Color(0xFFE2F6EE)
private val Green = Color(0xFF087F5B)
private val Red = Color(0xFFD34B55)

data class MoneyTx(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val account: String,
    val date: String = LocalDate.now().toString()
)
data class MoneyAccount(val name: String, val kind: String, val opening: Double = 0.0)
data class MoneyBudget(val category: String, val limit: Double)
data class AppData(
    val currency: String = "INR",
    val transactions: MutableList<MoneyTx> = mutableListOf(),
    val accounts: MutableList<MoneyAccount> = mutableListOf(
        MoneyAccount("Bank account", "Bank"), MoneyAccount("Cash wallet", "Cash")
    ),
    val budgets: MutableList<MoneyBudget> = mutableListOf(),
    val categories: MutableList<String> = mutableListOf(
        "Salary", "Other income", "Shopping", "Groceries", "Food & dining",
        "Transport", "Bills", "Rent & home", "Health", "Entertainment", "Other expense"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PennywiseApp(this) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PennywiseApp(context: Context) {
    val gson = remember { Gson() }
    var data by remember { mutableStateOf(loadData(context, gson)) }
    var page by remember { mutableStateOf("Home") }
    var showAdd by remember { mutableStateOf(false) }
    var showAccount by remember { mutableStateOf(false) }
    var showBudget by remember { mutableStateOf(false) }
    var txType by remember { mutableStateOf("Expense") }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Shopping") }
    var account by remember { mutableStateOf(data.accounts.firstOrNull()?.name ?: "Cash wallet") }
    var newAccount by remember { mutableStateOf("") }
    var accountKind by remember { mutableStateOf("Bank") }
    var budgetCategory by remember { mutableStateOf("Shopping") }
    var budgetAmount by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var snackbar by remember { mutableStateOf("") }

    fun persist(updated: AppData) {
        data = updated
        context.getSharedPreferences("pennywise_local", Context.MODE_PRIVATE)
            .edit().putString("app_data", gson.toJson(updated)).apply()
    }
    fun format(n: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }.format(n)
    val income = data.transactions.filter { it.type == "Income" }.sumOf { it.amount }
    val expenses = data.transactions.filter { it.type == "Expense" }.sumOf { it.amount }
    val balance = data.accounts.sumOf { it.opening } + income - expenses
    val monthPrefix = LocalDate.now().toString().take(7)
    val monthExpenses = data.transactions.filter { it.type == "Expense" && it.date.startsWith(monthPrefix) }.sumOf { it.amount }
    val monthIncome = data.transactions.filter { it.type == "Income" && it.date.startsWith(monthPrefix) }.sumOf { it.amount }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) try {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(gson.toJson(data)) }
            snackbar = "Backup exported"
        } catch (_: Exception) { snackbar = "Could not export backup" }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                val restored = gson.fromJson<AppData>(it, object : TypeToken<AppData>() {}.type)
                if (restored != null && restored.accounts != null && restored.transactions != null) {
                    persist(restored); snackbar = "Backup restored"
                } else snackbar = "Invalid backup file"
            }
        } catch (_: Exception) { snackbar = "Could not restore that file" }
    }

    MaterialTheme(colorScheme = lightColorScheme(
        primary = Purple, secondary = Green, background = Color(0xFFF5F7FB),
        surface = Color.White, error = Red
    )) {
        Scaffold(
            containerColor = Color(0xFFF5F7FB),
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(36.dp).background(Purple, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Text("₹", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("KharchaBook", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                                Text("YOUR MONEY, IN FOCUS", fontSize = 9.sp, color = Color.Gray, letterSpacing = 1.sp)
                            }
                        }
                    },
                    actions = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Purple, modifier = Modifier.padding(end = 14.dp).size(28.dp)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    listOf("Home", "Transactions", "Accounts", "Budgets", "Backup").forEach { dest ->
                        val icon = when(dest) {
                            "Home" -> Icons.Default.Home
                            "Transactions" -> Icons.Default.SwapVert
                            "Accounts" -> Icons.Default.AccountBalanceWallet
                            "Budgets" -> Icons.Default.PieChart
                            else -> Icons.Default.CloudDownload
                        }
                        NavigationBarItem(
                            selected = page == dest, onClick = { page = dest },
                            icon = { Icon(icon, contentDescription = dest) },
                            label = { Text(dest, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Purple, indicatorColor = Pale)
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = remember { SnackbarHostState() }) }
        ) { pad ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 18.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                when(page) {
                    "Home" -> {
                        item {
                            Text("YOUR FINANCES AT A GLANCE", color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                            Text("Good money habits start here.", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 29.sp)
                            Text("Track what comes in, what goes out, and what you can save.", color = Color.Gray, fontSize = 13.sp)
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard("Total balance", format(balance), "Across accounts", Modifier.weight(1f))
                                StatCard("This month", format(monthIncome-monthExpenses), "Income minus spending", Modifier.weight(1f))
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard("Total income", format(income), "Money received", Modifier.weight(1f), Green)
                                StatCard("Total expenses", format(expenses), "Money spent", Modifier.weight(1f), Red)
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { txType = "Income"; title = ""; amount = ""; showAdd = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("Add income") }
                                Button(onClick = { txType = "Expense"; title = ""; amount = ""; showAdd = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Purple)) { Icon(Icons.Default.Remove, null); Spacer(Modifier.width(4.dp)); Text("Add expense") }
                            }
                        }
                        item { SectionTitle("Recent transactions") }
                        if (data.transactions.isEmpty()) item { EmptyCard("No transactions yet. Add your first income or expense.") }
                        items(data.transactions.sortedByDescending { it.date }.take(6)) { tx -> TxRow(tx, ::format) }
                        item { SectionTitle("Monthly budgets") }
                        if (data.budgets.isEmpty()) item { EmptyCard("Budgets are optional. Add one whenever you're ready.") }
                        items(data.budgets) { b -> BudgetRow(b, data.transactions, ::format) }
                    }
                    "Transactions" -> {
                        item { Text("Transactions", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold); Text("Every rupee accounted for.", color = Color.Gray) }
                        item { OutlinedTextField(search, { search = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Search transactions") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true) }
                        item { Button(onClick = { txType = "Expense"; title = ""; amount = ""; showAdd = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text("Add transaction") } }
                        val filtered = data.transactions.sortedByDescending { it.date }.filter { "${it.title} ${it.category} ${it.account}".contains(search, true) }
                        if (filtered.isEmpty()) item { EmptyCard("No matching transactions.") }
                        items(filtered) { tx ->
                            TxRow(tx, ::format, onDelete = {
                                persist(data.copy(transactions = data.transactions.filterNot { it.id == tx.id }.toMutableList()))
                                snackbar = "Transaction deleted"
                            })
                        }
                    }
                    "Accounts" -> {
                        item { Text("Accounts", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold); Text("Bank and cash balances in one place.", color = Color.Gray) }
                        item { Button(onClick = { newAccount = ""; showAccount = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text("Add account") } }
                        items(data.accounts) { a ->
                            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, tonalElevation = 1.dp) {
                                Column(Modifier.fillMaxWidth().padding(17.dp)) {
                                    Text(if(a.kind == "Bank") "🏦  ${a.kind} account" else "👛  ${a.kind} account", color = Color.Gray, fontSize = 12.sp)
                                    Text(a.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    val bal = a.opening + data.transactions.filter { it.account == a.name }.sumOf { if(it.type == "Income") it.amount else -it.amount }
                                    Text(format(bal), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Purple)
                                    Text("Opening balance ${format(a.opening)}", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    "Budgets" -> {
                        item { Text("Budgets", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold); Text("Set monthly spending limits by category.", color = Color.Gray) }
                        item { Button(onClick = { budgetCategory = data.categories.firstOrNull() ?: "Shopping"; budgetAmount = ""; showBudget = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text("Set budget") } }
                        if (data.budgets.isEmpty()) item { EmptyCard("No budgets yet. Budgets are optional.") }
                        items(data.budgets) { b -> BudgetRow(b, data.transactions, ::format) }
                    }
                    "Backup" -> {
                        item { Text("Backup & restore", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold); Text("Keep a copy of your records that you control.", color = Color.Gray) }
                        item {
                            Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                                Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Local device storage", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Your records are saved on this device. Export a backup before changing phones or uninstalling the app.", color = Color.Gray, fontSize = 13.sp)
                                    Button(onClick = { exportLauncher.launch("KharchaBook-backup.json") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FileDownload, null); Spacer(Modifier.width(8.dp)); Text("Export backup file") }
                                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FileUpload, null); Spacer(Modifier.width(8.dp)); Text("Restore backup file") }
                                }
                            }
                        }
                        item {
                            Surface(shape = RoundedCornerShape(16.dp), color = Pale) {
                                Text("Google Drive automatic sync is not configured in this starter app. Backups are manual JSON files. Each person should keep their own backup private.", Modifier.padding(16.dp), color = Color(0xFF39327D), fontSize = 13.sp)
                            }
                        }
                        item {
                            Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("App info", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("KharchaBook", fontSize = 14.sp)
                                    Text("Developed by Safdar Masu", color = Color.Gray, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAdd) {
            AlertDialog(
                onDismissRequest = { showAdd = false },
                title = { Text("Add ${txType.lowercase()}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = txType == "Expense", onClick = { txType = "Expense"; category = "Shopping" }, label = { Text("Expense") })
                            FilterChip(selected = txType == "Income", onClick = { txType = "Income"; category = "Salary" }, label = { Text("Income") })
                        }
                        OutlinedTextField(title, { title = it }, label = { Text("Description") }, singleLine = true)
                        OutlinedTextField(amount, { amount = it }, label = { Text("Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                        var catExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                            OutlinedTextField(category, {}, readOnly = true, label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                            ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                                (if(txType == "Income") listOf("Salary", "Other income", "Freelance") else data.categories.filterNot { it in listOf("Salary", "Other income", "Freelance") }).forEach { c ->
                                    DropdownMenuItem(text = { Text(c) }, onClick = { category = c; catExpanded = false })
                                }
                            }
                        }
                        var acctExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = acctExpanded, onExpandedChange = { acctExpanded = it }) {
                            OutlinedTextField(account, {}, readOnly = true, label = { Text("Account") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(acctExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                            ExposedDropdownMenu(expanded = acctExpanded, onDismissRequest = { acctExpanded = false }) {
                                data.accounts.forEach { a -> DropdownMenuItem(text = { Text(a.name) }, onClick = { account = a.name; acctExpanded = false }) }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val value = amount.toDoubleOrNull()
                        if (title.isNotBlank() && value != null && value > 0 && data.accounts.isNotEmpty()) {
                            val tx = MoneyTx(title = title.trim(), amount = value, type = txType, category = category, account = account)
                            persist(data.copy(transactions = (data.transactions + tx).toMutableList()))
                            showAdd = false; snackbar = "Transaction saved"
                        } else snackbar = "Enter a description and valid amount"
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
            )
        }
        if (showAccount) {
            AlertDialog(
                onDismissRequest = { showAccount = false },
                title = { Text("Add account") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(newAccount, { newAccount = it }, label = { Text("Account name") }, singleLine = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = accountKind == "Bank", onClick = { accountKind = "Bank" }, label = { Text("Bank") })
                            FilterChip(selected = accountKind == "Cash", onClick = { accountKind = "Cash" }, label = { Text("Cash") })
                            FilterChip(selected = accountKind == "Wallet", onClick = { accountKind = "Wallet" }, label = { Text("Wallet") })
                        }
                    }
                },
                confirmButton = { TextButton(onClick = {
                    if(newAccount.isNotBlank() && data.accounts.none { it.name.equals(newAccount.trim(), true) }) {
                        val a = MoneyAccount(newAccount.trim(), accountKind)
                        persist(data.copy(accounts = (data.accounts + a).toMutableList())); account = a.name; showAccount = false
                    }
                }) { Text("Add") } },
                dismissButton = { TextButton(onClick = { showAccount = false }) { Text("Cancel") } }
            )
        }
        if (showBudget) {
            AlertDialog(
                onDismissRequest = { showBudget = false },
                title = { Text("Set monthly budget") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            OutlinedTextField(budgetCategory, {}, readOnly = true, label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                data.categories.filterNot { it in listOf("Salary", "Other income", "Freelance") }.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { budgetCategory = c; expanded = false }) }
                            }
                        }
                        OutlinedTextField(budgetAmount, { budgetAmount = it }, label = { Text("Limit (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    }
                },
                confirmButton = { TextButton(onClick = {
                    val v = budgetAmount.toDoubleOrNull()
                    if(v != null && v > 0) {
                        val updated = (data.budgets.filterNot { it.category == budgetCategory } + MoneyBudget(budgetCategory, v)).toMutableList()
                        persist(data.copy(budgets = updated)); showBudget = false
                    }
                }) { Text("Save budget") } },
                dismissButton = { TextButton(onClick = { showBudget = false }) { Text("Cancel") } }
            )
        }
    }
}

private fun loadData(context: Context, gson: Gson): AppData {
    return try {
        val raw = context.getSharedPreferences("pennywise_local", Context.MODE_PRIVATE).getString("app_data", null)
        if (raw.isNullOrBlank()) AppData() else gson.fromJson(raw, AppData::class.java) ?: AppData()
    } catch (_: Exception) { AppData() }
}

@Composable
private fun StatCard(label: String, value: String, foot: String, modifier: Modifier = Modifier, valueColor: Color = Color(0xFF172033)) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color.White, tonalElevation = 1.dp) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = valueColor, lineHeight = 24.sp)
            Spacer(Modifier.height(5.dp))
            Text(foot, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun EmptyCard(message: String) {
    Surface(shape = RoundedCornerShape(15.dp), color = Color.White) {
        Text(message, Modifier.padding(18.dp), color = Color.Gray, fontSize = 13.sp)
    }
}

@Composable
private fun TxRow(tx: MoneyTx, format: (Double) -> String, onDelete: (() -> Unit)? = null) {
    Surface(shape = RoundedCornerShape(15.dp), color = Color.White, tonalElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(Pale, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text(if(tx.type == "Income") "↗" else "↘", color = if(tx.type == "Income") Green else Purple, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(tx.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("${tx.category} · ${tx.account}", color = Color.Gray, fontSize = 10.sp)
                Text(tx.date, color = Color.Gray, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text((if(tx.type == "Income") "+" else "−") + format(tx.amount), fontWeight = FontWeight.Bold, color = if(tx.type == "Income") Green else Red, fontSize = 12.sp)
                if(onDelete != null) TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp)) { Text("Delete", fontSize = 10.sp, color = Red) }
            }
        }
    }
}

@Composable
private fun BudgetRow(budget: MoneyBudget, transactions: List<MoneyTx>, format: (Double) -> String) {
    val month = LocalDate.now().toString().take(7)
    val spent = transactions.filter { it.type == "Expense" && it.category == budget.category && it.date.startsWith(month) }.sumOf { it.amount }
    val progress = (spent / budget.limit).toFloat().coerceIn(0f, 1f)
    Surface(shape = RoundedCornerShape(15.dp), color = Color.White, tonalElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(budget.category, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("${format(spent)} / ${format(budget.limit)}", fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(9.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(7.dp), color = if(progress >= .8f) Red else Purple, trackColor = Pale)
            Spacer(Modifier.height(5.dp))
            Text(if(spent > budget.limit) "Over budget" else "${format(budget.limit-spent)} remaining", color = Color.Gray, fontSize = 10.sp)
        }
    }
}
