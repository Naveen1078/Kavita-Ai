package com.kavita.ai

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class KavitaService : NotificationListenerService() {

    private val seen = LinkedHashSet<String>()
    private val memory = mutableMapOf<String, MutableList<String>>()
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val prefs = getSharedPreferences("kavita", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) return
        if (sbn.packageName != "com.whatsapp") return

        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val sender = n.extras.getString(Notification.EXTRA_TITLE) ?: return
        val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

        val key = "$sender|$text|${sbn.postTime}"
        if (!seen.add(key)) return
        if (seen.size > 200) seen.remove(seen.first())

        val action = n.actions?.firstOrNull { it.remoteInputs?.isNotEmpty() == true } ?: return

        scope.launch {
            try {
                val history = memory.getOrPut(sender) { mutableListOf() }
                history.add("सामने वाला: $text")
                val rule = prefs.getString("rule_$sender", "दोस्ताना और छोटा लहजा") ?: ""
                val apiKey = prefs.getString("api_key", "") ?: ""
                val reply = askGemini(apiKey, rule, history.takeLast(10))
                history.add("Kavita: $reply")
                sendReply(action, reply)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendReply(action: Notification.Action, text: String) {
        val ri = action.remoteInputs[0]
        val intent = Intent()
        val bundle = Bundle().apply { putCharSequence(ri.resultKey, text) }
        RemoteInput.addResultsToIntent(action.remoteInputs, intent, bundle)
        action.actionIntent.send(this, 0, intent)
    }

    private fun askGemini(key: String, rule: String, hist: List<String>): String {
        val prompt = "तुम Kavita हो, एक AI असिस्टेंट। नियम: $rule। " +
            "जिस भाषा में message आया उसी भाषा में छोटा जवाब दो।\n" +
            hist.joinToString("\n")
        val body = JSONObject().put("contents", JSONArray().put(
            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key")
        val c = url.openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json")
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        val res = JSONObject(c.inputStream.bufferedReader().readText())
        return res.getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts")
            .getJSONObject(0).getString("text")
    }
}
