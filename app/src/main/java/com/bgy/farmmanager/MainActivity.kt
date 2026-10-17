package com.bgy.farmmanager

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Purple = Color(0xFF8B5CF6)
private val Dark = Color(0xFF101018)
private val CardBg = Color(0xFF1D1B2A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("wheat_helper", MODE_PRIVATE)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Purple, background = Dark, surface = CardBg)) {
                var harvest by remember { mutableStateOf(prefs.getString("harvest_points", "300,500;360,500;420,500") ?: "") }
                var sow by remember { mutableStateOf(prefs.getString("sow_points", "300,500;360,500;420,500") ?: "") }
                var sell by remember { mutableStateOf(prefs.getString("sell_points", "500,900;600,700;600,800") ?: "") }
                var tapDelay by remember { mutableStateOf(prefs.getLong("delay_ms", 900L).toString()) }
                var growDelay by remember { mutableStateOf(prefs.getLong("grow_delay_ms", 1500L).toString()) }
                var running by remember { mutableStateOf(prefs.getBoolean("running", false)) }
                var status by remember { mutableStateOf("اضبط الإحداثيات على شاشة هاتفك قبل التشغيل.") }
                Column(Modifier.fillMaxSize().background(Dark).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("WHEAT LOOP", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("HAY DAY • ANDROID ACCESSIBILITY", color = Purple, fontSize = 12.sp)
                    Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("1. حصاد القمح", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("إحداثيات X,Y مفصولة بفاصلة منقوطة. اضغط على حقول الحصاد أثناء المعايرة.", color = Color.LightGray, fontSize = 12.sp)
                            OutlinedTextField(harvest, { harvest = it }, label = { Text("Harvest taps: x,y;x,y") }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("2. إعادة زراعة القمح", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("ضع إحداثيات مناطق الحقول الفارغة. تحتاج لضبطها يدويًا حسب جهازك.", color = Color.LightGray, fontSize = 12.sp)
                            OutlinedTextField(sow, { sow = it }, label = { Text("Sow taps: x,y;x,y") }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("3. متجر الطريق", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("سلسلة نقرات مهيأة يدويًا لفتح المتجر وإعداد البيع. قد تختلف الشاشة أو تتغير الخانات؛ راقب التشغيل.", color = Color.LightGray, fontSize = 12.sp)
                            OutlinedTextField(sell, { sell = it }, label = { Text("Shop taps: x,y;x,y") }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    OutlinedTextField(tapDelay, { tapDelay = it.filter(Char::isDigit) }, label = { Text("الانتظار بين النقرات (مللي ثانية)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(growDelay, { growDelay = it.filter(Char::isDigit) }, label = { Text("الانتظار قبل دورة جديدة (ثانية)") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        prefs.edit().putString("harvest_points", harvest).putString("sow_points", sow).putString("sell_points", sell)
                            .putLong("delay_ms", (tapDelay.toLongOrNull() ?: 900L).coerceIn(350L, 10000L))
                            .putLong("grow_delay_ms", (growDelay.toLongOrNull() ?: 1500L).coerceIn(10L, 86400L) * 1000L)
                            .apply()
                        status = "تم حفظ إعدادات المعايرة على هذا الجهاز."
                    }, modifier = Modifier.fillMaxWidth()) { Text("حفظ الإعدادات") }
                    OutlinedButton(onClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) { Text("تفعيل خدمة إمكانية الوصول") }
                    Button(onClick = {
                        val h = parsePoints(harvest); val s = parsePoints(sow); val v = parsePoints(sell)
                        if (h.isEmpty() || s.isEmpty() || v.isEmpty()) {
                            status = "أدخل إحداثيات صحيحة لكل مرحلة أولًا."
                        } else {
                            prefs.edit().putString("harvest_points", harvest).putString("sow_points", sow).putString("sell_points", sell)
                                .putString("points", (h + s + v).joinToString(";") { "${it.first},${it.second}" })
                                .putInt("harvest_count", h.size).putInt("sow_count", s.size).putInt("sell_count", v.size)
                                .putLong("delay_ms", (tapDelay.toLongOrNull() ?: 900L).coerceIn(350L, 10000L))
                                .putLong("grow_delay_ms", (growDelay.toLongOrNull() ?: 1500L).coerceIn(10L, 86400L) * 1000L)
                                .putBoolean("running", true).apply()
                            running = true
                            status = "بدأت دورة النقر. راقب الشاشة؛ النقرات لا تكتشف نضج القمح تلقائيًا."
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("▶ بدء الدورة") }
                    Button(onClick = {
                        prefs.edit().putBoolean("running", false).apply()
                        running = false
                        status = "تم طلب الإيقاف."
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)), modifier = Modifier.fillMaxWidth()) { Text("■ إيقاف") }
                    Text(if (running) "الحالة: تشغيل" else "الحالة: متوقف", color = if (running) Color(0xFF4ADE80) else Color.LightGray, fontWeight = FontWeight.Bold)
                    Text(status, color = Color.White, fontSize = 13.sp)
                    Text("تنبيه: هذه أداة نقرات بإحداثيات يحددها المستخدم، وليست رؤية آلية للعبة. لا تضمن اكتشاف المحصول الناضج أو إكمال البيع تلقائيًا؛ اختبرها أولًا مع مراقبة مباشرة.", color = Color(0xFFFBBF24), fontSize = 12.sp)
                }
            }
        }
    }

    private fun parsePoints(raw: String): List<Pair<Float, Float>> =
        raw.split(";").mapNotNull { row ->
            val p = row.trim().split(",")
            if (p.size != 2) null else {
                val x = p[0].trim().toFloatOrNull()
                val y = p[1].trim().toFloatOrNull()
                if (x == null || y == null || x < 0 || y < 0) null else x to y
            }
        }
}
