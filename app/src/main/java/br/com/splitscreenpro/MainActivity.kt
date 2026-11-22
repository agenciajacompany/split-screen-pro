package br.com.splitscreenpro

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.Collator
import java.util.Locale

class MainActivity : Activity() {
    data class App(val name: String, val component: ComponentName) {
        val id: String get() = component.flattenToString()
        fun intent() = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(component)
    }
    private val bg = Color.rgb(16,24,39)
    private val panel = Color.rgb(29,43,64)
    private val blue = Color.rgb(32,83,151)
    private val muted = Color.rgb(174,190,211)
    private var apps = emptyList<App>()
    private var first: App? = null
    private var second: App? = null
    private var pending = false
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("pairs", MODE_PRIVATE) }
    private lateinit var firstButton: Button
    private lateinit var secondButton: Button
    private lateinit var launchButton: Button
    private lateinit var saveButton: Button
    private lateinit var replaceButton: Button
    private lateinit var status: TextView
    private lateinit var favorites: LinearLayout
    private val timeout = Runnable {
        if (pending) {
            pending = false
            status.text = "O Android não confirmou a divisão. Tente novamente ou use o modo manual."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadApps()
        first = apps.find { it.id == prefs.getString("first", "") }
        second = apps.find { it.id == prefs.getString("second", "") }
        buildView()
        refresh()
    }
    private fun loadApps() {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val collator = Collator.getInstance(Locale("pt", "BR"))
        apps = packageManager.queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName != packageName && it.activityInfo.exported && it.activityInfo.enabled }
            .map { App(it.loadLabel(packageManager).toString(), ComponentName(it.activityInfo.packageName, it.activityInfo.name)) }
            .distinctBy { it.id }.sortedWith { a, b -> collator.compare(a.name, b.name) }
    }
    private fun dp(x: Int) = (x * resources.displayMetrics.density + 0.5f).toInt()
    private fun shape(color: Int, radius: Int = 14) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun text(value: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); if (bold) setTypeface(null, Typeface.BOLD)
    }
    private fun button(value: String, primary: Boolean = false, click: () -> Unit) = Button(this).apply {
        text = value; isAllCaps = false; textSize = 15f; setTextColor(Color.WHITE)
        background = shape(if (primary) blue else panel); minHeight = dp(52); setPadding(dp(12), dp(8), dp(12), dp(8))
        setOnClickListener { click() }
    }
    private fun add(parent: LinearLayout, view: View, top: Int = 8) {
        parent.addView(view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top) })
    }
    private fun buildView() {
        val scroll = ScrollView(this).apply { isFillViewport = true; setBackgroundColor(bg) }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20),dp(20),dp(20),dp(24)) }
        scroll.addView(root)
        add(root, text("SPLIT SCREEN PRO", 12f, muted, true), 0)
        add(root, text("Dois apps. Uma tela.", 26f, bold = true), 12)
        add(root, text("Escolha dois aplicativos para abrir lado a lado ou um acima do outro.", 15f, muted), 6)
        add(root, text("PRIMEIRO APLICATIVO", 12f, muted, true), 24)
        firstButton = button("Selecionar aplicativo") { select(true) }; add(root, firstButton)
        add(root, text("SEGUNDO APLICATIVO", 12f, muted, true), 16)
        secondButton = button("Selecionar aplicativo") { select(false) }; add(root, secondButton)
        add(root, button("Inverter ordem") { val old = first; first = second; second = old; refresh() })
        launchButton = button("Abrir tela dividida", true) { launchPair() }; add(root, launchButton, 18)
        replaceButton = button("Abrir primeiro app nesta janela", true) { openFirstInCurrentWindow() }; add(root, replaceButton)
        saveButton = button("Salvar combinação") { savePair() }; add(root, saveButton)
        status = text("A divisão é controlada pelo Android. Gire o celular para alternar a disposição, quando disponível.", 13f, muted)
        add(root, status, 12)
        add(root, button("Modo manual / ajuda") { showHelp() }, 14)
        add(root, text("FAVORITOS", 12f, muted, true), 24)
        favorites = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; add(root, favorites, 2)
        add(root, text("Versão 0.1 • Sem anúncios • Sem acesso à internet", 12f, muted), 24)
        setContentView(scroll)
    }
    private fun refresh() {
        firstButton.text = first?.name ?: "Selecionar aplicativo"
        secondButton.text = second?.name ?: "Selecionar aplicativo"
        val valid = first != null && second != null && first?.component?.packageName != second?.component?.packageName
        launchButton.isEnabled = valid; launchButton.alpha = if (valid) 1f else .45f
        saveButton.isEnabled = valid; saveButton.alpha = if (valid) 1f else .45f
        replaceButton.visibility = if (isInMultiWindowMode && first != null) View.VISIBLE else View.GONE
        prefs.edit().putString("first", first?.id).putString("second", second?.id).apply()
        renderFavorites()
    }
    private fun select(isFirst: Boolean) {
        loadApps()
        val available = apps.filter { it.component.packageName != (if (isFirst) second else first)?.component?.packageName }
        if (available.isEmpty()) { toast("Nenhum outro aplicativo disponível."); return }
        val adapter = object : ArrayAdapter<App>(this, android.R.layout.simple_list_item_1, available) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val row = super.getView(position, convertView, parent) as TextView
                val app = getItem(position)!!
                row.text = app.name
                val icon = runCatching { packageManager.getActivityIcon(app.component) }.getOrNull()
                icon?.setBounds(0,0,dp(30),dp(30)); row.setCompoundDrawables(icon,null,null,null); row.compoundDrawablePadding = dp(12)
                return row
            }
        }
        AlertDialog.Builder(this).setTitle("Escolha um aplicativo").setAdapter(adapter) { _, index ->
            if (isFirst) first = available[index] else second = available[index]
            refresh()
        }.setNegativeButton("Cancelar", null).show()
    }
    private fun launchPair() {
        val other = second ?: return
        if (first == null || first?.component?.packageName == other.component.packageName) return
        pending = !isInMultiWindowMode
        status.text = "Solicitando tela dividida ao Android…"
        handler.removeCallbacks(timeout)
        try {
            startActivity(other.intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT))
            if (pending) handler.postDelayed(timeout, 4500)
            else handler.postDelayed({ openFirstInCurrentWindow() }, 650)
        } catch (e: Exception) { pending = false; status.text = "Não foi possível abrir o segundo aplicativo. Use o modo manual." }
    }
    override fun onMultiWindowModeChanged(inMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(inMultiWindowMode, newConfig)
        if (!::replaceButton.isInitialized) return
        refresh()
        if (pending && inMultiWindowMode) {
            pending = false; handler.removeCallbacks(timeout)
            status.text = "Tela dividida detectada. Abrindo o primeiro app…"
            handler.postDelayed({ openFirstInCurrentWindow() }, 650)
        }
    }
    private fun openFirstInCurrentWindow() {
        if (!isInMultiWindowMode || isFinishing || isDestroyed) {
            if (!isDestroyed) status.text = "A divisão não está ativa. Use o modo manual."
            return
        }
        val app = first ?: return
        try {
            status.text = "Se o primeiro app abrir em tela cheia, use o modo manual."
            startActivity(app.intent())
        } catch (e: Exception) { status.text = "Este aplicativo não pôde ser aberto nesta janela. Use o modo manual." }
    }
    private fun showHelp() {
        AlertDialog.Builder(this).setTitle("Divisão manual")
            .setMessage("1. Abra o primeiro aplicativo.\n2. Abra os aplicativos recentes.\n3. Toque no ícone do primeiro app e escolha Tela dividida.\n4. Selecione o segundo aplicativo: ${second?.name ?: "o app desejado"}.\n5. Gire o celular se quiser usar os apps lado a lado.\n\nO Android decide quais apps podem compartilhar a tela.")
            .setPositiveButton("Abrir primeiro app") { _, _ ->
                first?.let { app -> try { startActivity(app.intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { toast("Não foi possível abrir o aplicativo.") } }
            }.setNegativeButton("Fechar", null).show()
    }
    private fun pairs(): JSONArray = runCatching { JSONArray(prefs.getString("favorites", "[]") ?: "[]") }.getOrDefault(JSONArray())
    private fun savePair() {
        val a = first ?: return; val b = second ?: return
        val all = pairs()
        if ((0 until all.length()).any { all.optJSONObject(it)?.let { p -> p.optString("a") == a.id && p.optString("b") == b.id } == true }) { toast("Combinação já salva."); return }
        all.put(JSONObject().put("a",a.id).put("b",b.id))
        prefs.edit().putString("favorites",all.toString()).apply(); renderFavorites(); toast("Combinação salva.")
    }
    private fun renderFavorites() {
        favorites.removeAllViews(); val all = pairs()
        for (i in 0 until all.length()) {
            val p = all.optJSONObject(i) ?: continue
            val a = apps.find { it.id == p.optString("a") }; val b = apps.find { it.id == p.optString("b") }
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val open = button(if (a != null && b != null) "${a.name} + ${b.name}" else "App não disponível") {
                if (a != null && b != null) { first = a; second = b; refresh(); launchPair() }
            }; open.isEnabled = a != null && b != null
            row.addView(open, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val delete = button("×") {
                val updated = JSONArray(); for (j in 0 until all.length()) if (j != i) updated.put(all.get(j))
                prefs.edit().putString("favorites", updated.toString()).apply(); renderFavorites()
            }; delete.contentDescription = "Excluir combinação"; row.addView(delete, LinearLayout.LayoutParams(dp(48), ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(6) })
            add(favorites, row)
        }
        if (all.length() == 0) add(favorites, text("Nenhuma combinação salva ainda.", 14f, muted))
    }
    private fun toast(message: String) = Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy() }
}
