package com.psychedelicshayna.krypton

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast

import com.psychedelicshayna.krypton.VaultViewer;

const val REQ_PICK_FILE:Int = 1;

class MainActivity : AppCompatActivity() {
    // Request code for selecting a PDF document.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.btLoadVault)

        button.setOnClickListener {
//            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
//                addCategory(Intent.CATEGORY_OPENABLE)
//                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//                type = "*/*"
//            }
//
//            startActivityForResult(intent, 1)

            val intent:Intent = Intent(this, VaultViewer::class.java)
            startActivity(intent)
        };
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == RESULT_OK && requestCode == REQ_PICK_FILE) {
            if(data != null) {
                Toast.makeText(this@MainActivity, data.dataString, Toast.LENGTH_SHORT).show()
            }
        }
    }
}