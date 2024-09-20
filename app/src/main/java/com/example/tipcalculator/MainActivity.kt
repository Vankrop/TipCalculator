package com.example.tipcalculator

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tipcalculator.ui.theme.TipCalculatorTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private val viewModel: TipCalculatorViewModel by viewModels() // Get ViewModel instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Register the permission launcher
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getLastLocation()
            }
        }

        setContent {
            TipCalculatorTheme {
                val navController = rememberNavController()

                // State collections from ViewModel
                val billAmount by viewModel.billAmount.collectAsState()
                val applyTaxesState by viewModel.applyTaxesState.collectAsState()
                val tipPercentState by viewModel.tipPercentState.collectAsState()
                val numberOfPeopleState by viewModel.numberOfPeopleState.collectAsState()
                val tipAmount by viewModel.tipAmount.collectAsState()
                val totalWithTip by viewModel.totalBillAmount.collectAsState()
                val latitude by viewModel.latitude.collectAsState()
                val longitude by viewModel.longitude.collectAsState()
                val tips by viewModel.tipsList.collectAsState() // Get tips from ViewModel

                // Check for permissions and get location
                LaunchedEffect(Unit) {
                    when {
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                            getLastLocation()
                        }
                        else -> {
                            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                }

                NavHost(navController = navController, startDestination = "bill_amount") {
                    composable("bill_amount") {
                        BillAmountScreen(
                            modifier = Modifier.padding(16.dp),
                            billAmount = billAmount,
                            onBillAmountChange = viewModel::onTextFieldValueChange,
                            applyTaxesState = applyTaxesState,
                            onApplyTaxesChange = viewModel::onApplyTaxesChange,
                            tipPercentState = tipPercentState,
                            onTipPercentChange = viewModel::onTipPercentChange,
                            numberOfPeopleState = numberOfPeopleState,
                            onNumberOfPeopleChange = { viewModel.onNumberOfPeopleChange(it.toInt()) },
                            tipAmount = tipAmount,
                            totalWithTip = totalWithTip,
                            latitude = latitude,
                            onLatitudeChange = viewModel::onLatitudeChange,
                            longitude = longitude,
                            onLongitudeChange = viewModel::onLongitudeChange,
                            onOpenMap = { lat, lon -> openMap(lat, lon) },
                            onAddTip = { tip ->
                                viewModel.addTip(tip) // Add tip to database
                            },
                            onViewTipsClick = {
                                navController.navigate("tips_list")
                            }
                        )
                    }
                    composable("tips_list") {
                        TipsListScreen(
                            tips = tips,
                            onBackClick = { navController.popBackStack() },
                            onTipSelected = { selectedTip ->
                                viewModel.onTextFieldValueChange(selectedTip.billAmount.toString())
                                viewModel.onApplyTaxesChange(selectedTip.taxesApplied)
                                viewModel.onTipPercentChange(selectedTip.tipPercentage.toFloat())
                                viewModel.onNumberOfPeopleChange(selectedTip.numberOfPersons)
                                viewModel.onLatitudeChange(selectedTip.latitude.toString())
                                viewModel.onLongitudeChange(selectedTip.longitude.toString())

                                // Navigate back to BillAmountScreen
                                navController.popBackStack() // Pop TipsListScreen
                            },
                            onTipDelete = { tip ->
                                viewModel.deleteTip(tip) // Delete tip from database
                            }
                        )
                    }
                }

                // Load tips from database when the activity starts
                LaunchedEffect(Unit) {
                    viewModel.fetchTips()
                }
            }
        }
    }

    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    // Update ViewModel with the current location
                    viewModel.onLatitudeChange(it.latitude.toString())
                    viewModel.onLongitudeChange(it.longitude.toString())
                }
            }
        }
    }

    private fun openMap(latitude: String, longitude: String) {
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude (Location)")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(fallbackIntent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipsListScreen(tips: List<Tip>, onBackClick: () -> Unit, onTipSelected: (Tip) -> Unit, onTipDelete: (Tip) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tips List") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            items(tips) { tip ->
                TipItem(tip, onTipSelected) {
                    onTipDelete(tip) // Delete tip when clicked
                }
            }
        }
    }
}

@Composable
fun TipItem(tip: Tip, onTipSelected: (Tip) -> Unit, onDelete: () -> Unit) {
    Column(modifier = Modifier
        .padding(16.dp)
        .clickable(onClick = { onTipSelected(tip) })) {
        Text("Bill Amount: $${"%.2f".format(tip.billAmount)}")
        Text("Tip Percentage: ${tip.tipPercentage}%")
        Text("Number of Persons: ${tip.numberOfPersons}")
        Text("Latitude: ${tip.latitude}")
        Text("Longitude: ${tip.longitude}")
        Text("Taxes Applied: ${if (tip.taxesApplied) "Yes" else "No"}")
        Button(onClick = onDelete, modifier = Modifier.padding(top = 8.dp)) {
            Text("Delete")
        }
        Divider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
fun BillAmountScreen(
    modifier: Modifier = Modifier,
    billAmount: String,
    onBillAmountChange: (String) -> Unit,
    applyTaxesState: Boolean,
    onApplyTaxesChange: (Boolean) -> Unit,
    tipPercentState: Float,
    onTipPercentChange: (Float) -> Unit,
    numberOfPeopleState: Int,
    onNumberOfPeopleChange: (Int) -> Unit,
    tipAmount: Double,
    totalWithTip: Double,
    latitude: String,
    onLatitudeChange: (String) -> Unit,
    longitude: String,
    onLongitudeChange: (String) -> Unit,
    onOpenMap: (String, String) -> Unit,
    onAddTip: (Tip) -> Unit,
    onViewTipsClick: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        item { BillAmountInput(billAmount, onBillAmountChange) }
        item { ApplyTaxesSwitch(applyTaxesState, onApplyTaxesChange) }
        item { TipPercentSlider(tipPercentState, onTipPercentChange) }
        item { TipAmountDisplay(tipAmount) }
        item { TotalWithTipDisplay(totalWithTip) }
        item { NumberOfPeopleSlider(numberOfPeopleState, onNumberOfPeopleChange) }
        item { LatitudeInput(latitude, onLatitudeChange) }
        item { LongitudeInput(longitude, onLongitudeChange) }
        item {
            Row(modifier = Modifier.padding(top = 16.dp)) {
                Button(
                    onClick = { onOpenMap(latitude, longitude) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.openMap))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        // Create the Tip object and call onAddTip
                        val tip = Tip(
                            billAmount = billAmount.toDoubleOrNull() ?: 0.0,
                            tipPercentage = tipPercentState.toDouble(),
                            numberOfPersons = numberOfPeopleState,
                            latitude = latitude.toDoubleOrNull() ?: 0.0,
                            longitude = longitude.toDoubleOrNull() ?: 0.0,
                            taxesApplied = applyTaxesState
                        )
                        onAddTip(tip) // Add the tip to the database
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.addTip))
                }
            }
        }


        item {
            Button(
                onClick = onViewTipsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(text = stringResource(R.string.viewTips))
            }
        }
    }
}

@Composable
fun BillAmountInput(billAmount: String, onBillAmountChange: (String) -> Unit) {
    Column {
        TextField(
            value = billAmount,
            onValueChange = onBillAmountChange,
            label = { Text(stringResource(R.string.billAmount)) },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Decimal
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ApplyTaxesSwitch(applyTaxesState: Boolean, onApplyTaxesChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.padding(top = 16.dp)) {
        Text(text = stringResource(R.string.applyTaxes), fontSize = 18.sp)
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = applyTaxesState,
            onCheckedChange = onApplyTaxesChange
        )
    }
}

@Composable
fun TipPercentSlider(tipPercentState: Float, onTipPercentChange: (Float) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.tipPercent),
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        Slider(
            value = tipPercentState,
            onValueChange = onTipPercentChange,
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "${tipPercentState.toInt()}%",
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun NumberOfPeopleSlider(numberOfPeopleState: Int, onValueChange: (Int) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.numberOfPeople),
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        Slider(
            value = numberOfPeopleState.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 1f..10f,
            steps = 9,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "${numberOfPeopleState}",
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun LatitudeInput(latitude: String, onLatitudeChange: (String) -> Unit) {
    Column {
        TextField(
            value = latitude,
            onValueChange = onLatitudeChange,
            label = { Text("Latitude") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun LongitudeInput(longitude: String, onLongitudeChange: (String) -> Unit) {
    Column {
        TextField(
            value = longitude,
            onValueChange = onLongitudeChange,
            label = { Text("Longitude") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun TipAmountDisplay(tipAmount: Double) {
    Column {
        Text(
            text = stringResource(R.string.tipAmount),
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        TextField(
            value = "%.2f".format(tipAmount),
            onValueChange = {},
            label = { Text(stringResource(R.string.tipAmount)) },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun TotalWithTipDisplay(totalWithTip: Double) {
    Column {
        Row(modifier = Modifier.padding(top = 16.dp)) {
            Text(
                text = stringResource(R.string.totalWithTip),
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$%.2f".format(totalWithTip),
                fontSize = 18.sp
            )
        }
    }
}
