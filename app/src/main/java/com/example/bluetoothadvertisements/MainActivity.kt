/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluetoothadvertisements

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bluetoothadvertisements.databinding.ActivityMainBinding
//import com.example.bluetoothadvertisements.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import com.google.firebase.ktx.Firebase
import com.google.android.gms.location.LocationRequest.create
import com.google.firebase.firestore.ktx.firestore
private const val TAG = "MainActivity"

/**
 * Demos how to advertise a bluetooth device and also how to scan for remote nearby bluetooth
 * devices.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
private lateinit var fusedLocationClient: FusedLocationProviderClient
private lateinit var locationCallback: LocationCallback

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            checkPermissions()
        }
        fusedLocationClient.requestLocationUpdates(
            LocationRequest().apply {
                interval = 10000
                fastestInterval = 5000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            },
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun checkPermissions() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "nie dziala", Toast.LENGTH_LONG).show()
            return
        }
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS, Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE)
        val permissionDeniedList: MutableList<String> = ArrayList()
        for (permission in permissions) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, permission)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
//                onPermissionGranted(permission)
                Log.d(TAG, "Permission granted: $permission")
            } else {
                permissionDeniedList.add(permission)
            }
        }
        if (permissionDeniedList.isNotEmpty()) {
            val deniedPermissions = permissionDeniedList.toTypedArray()
            ActivityCompat.requestPermissions(
                this,
                deniedPermissions,
                2
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var name = "test"
        binding = ActivityMainBinding.inflate(layoutInflater)
        //setContentView

        setContentView( binding.root )


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            checkPermissions()
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                for (location in locationResult.locations){
                    val currTime: Long = System.currentTimeMillis()

                    val bluetoothInstance = hashMapOf(
                        "time" to currTime.toString(),
                        "y" to location.latitude.toString(),
                        "x" to location.longitude.toString(),
                        "gps" to true
                    )
//                    val name = findViewById<EditText>(R.id.editTextTextPersonName).text.toString()
                    val db = Firebase.firestore
                    db.collection(  name +" | " + (currTime / 3600000).toInt().toString())
                        .document(currTime.toString()).
                        set(bluetoothInstance)

                    Log.d("aaaaaa", "onLocationResult: ${location.latitude} ${location.longitude}")
//
                }
            }

        }

        startLocationUpdates()

        if (savedInstanceState == null) {
            verifyBluetoothCapabilities()
        }
        findViewById<Button>(R.id.button).setOnClickListener {
            val bluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
            bluetoothAdapter.name = findViewById<EditText>(R.id.editTextTextPersonName).text.toString()
            name = findViewById<EditText>(R.id.editTextTextPersonName).text.toString()

        }
    }

    private fun verifyBluetoothCapabilities() {
        val bluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            checkPermissions()
        }
        when {
            bluetoothAdapter == null ->
                // Bluetooth is not supported on this hardware platform
                showErrorText("onCreate: bluetooth not supported")
            !bluetoothAdapter.isEnabled -> // Bluetooth is OFF, user should turn it ON
                // Prompt the use to allow the app to turn on Bluetooth
                startActivityForResult(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    REQUEST_ENABLE_BT
                )
            bluetoothAdapter.isEnabled && !bluetoothAdapter.isMultipleAdvertisementSupported ->
                showErrorText("Bluetooth Advertisements are not supported.")
            bluetoothAdapter.isEnabled && bluetoothAdapter.isMultipleAdvertisementSupported ->
                setupFragments()

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_ENABLE_BT) {
            Log.d(TAG, "onActivityResult: REQUEST_ENABLE_BT")
            verifyBluetoothCapabilities()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * Setup two Fragments in the Activity: one shows the list of nearby devices; one shows the
     * switch for advertising to nearby devices.
     */
    private fun setupFragments() {
        val fragTransaction = supportFragmentManager.beginTransaction()
        Log.d(TAG, "setupFragments: setting up fragments")
        fragTransaction.replace(R.id.advertiser_fragment_container, AdvertiserFragment())
        fragTransaction.commit()
    }

    private fun showErrorText(msg: String) {
        Log.d(TAG, "showErrorText: $msg")
    }
}
