package com.kavita.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("kavita", Context.MODE_PRIVATE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val title = TextView(this).apply {
            text = "Kavita AI"
            textSize = 24f
        }

        val keyBox = EditText(this).apply {
            hint = "Gemini API key यहाँ डालो"
            setText(prefs.getString("api_key", ""))
        }

        val saveBtn = Button(this).apply {
            text = "API key सेव करो"
            setOnClickListener {
                prefs.edit().putString("api_key", keyBox.text.toString().trim()).apply()
                Toast.makeText(context, "सेव हो गई", Toast.LENGTH_SHORT).show()
            }
        }

        val toggle = Switch(this).apply {
            text = "ऑटो-रिप्लाई चालू"
            isChecked = prefs.getBoolean("enabled", false)
            setOnCheckedChangeListener { _, on ->
                prefs.edit().putBoolean("enabled", on).apply()
            }
        }

        val accessBtn = Button(this).apply {
            text = "Notification access खोलो"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        layout.addView(title)
        layout.addView(keyBox)
        layout.addView(saveBtn)
        layout.addView(toggle)
        layout.addView(accessBtn)
        setContentView(layout)
    }
}
