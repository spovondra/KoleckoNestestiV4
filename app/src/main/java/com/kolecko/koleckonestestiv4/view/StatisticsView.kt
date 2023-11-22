package com.kolecko.koleckonestestiv4

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.helper.StaticLabelsFormatter
import com.jjoe64.graphview.series.BarGraphSeries
import com.jjoe64.graphview.series.DataPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// Třída pro zobrazování statistik v uživatelském rozhraní
class StatisticsViewImpl : AppCompatActivity() {

    private lateinit var graphView: GraphView
    private lateinit var updateGraphButton: Button
    private lateinit var controller: StatisticsController
    private lateinit var database: AppDatabase
    private var pointCounter: Int = 0
    private lateinit var lastAddedDate: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        // Nastavení akční lišty
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Statistiky"
        supportActionBar?.elevation = 0f

        // Inicializace grafu, tlačítka a kontrolleru
        graphView = findViewById(R.id.graph)
        updateGraphButton = findViewById(R.id.clearDataButton)

        // Inicializace databáze a kontroleru s použitím datového rozhraní
        database = AppDatabase.getInstance(this)
        controller = StatisticsController(DataRepository(database.dataDao()))
        lastAddedDate = getCurrentDate()

        // Načtení aktuálních dat
        GlobalScope.launch {
            val currentDate = getCurrentDate()
            val dataEntity = controller.getDataByDate(currentDate)

            // Pokud jsou data pro aktuální den k dispozici, použijte je pro inicializaci čítače bodů
            if (dataEntity != null) {
                pointCounter = dataEntity.value.toInt()
            }

            // Aktualizace grafu
            updateGraph()
        }

        // Nastavení posluchače pro tlačítko aktualizace grafu
        updateGraphButton.setOnClickListener {
            val currentDate = getCurrentDate()

            // Kontrola, zda začal nový den
            if (currentDate != lastAddedDate) {
                pointCounter = 0
                lastAddedDate = currentDate
            }

            val value = pointCounter.toDouble()

            // Vložení dat a aktualizace grafu v novém coroutine
            GlobalScope.launch {
                controller.insertOrUpdateData(currentDate, value)
                pointCounter++
                updateGraph()
            }
        }
    }

    // Metoda pro získání aktuálního data ve formátu "yyyy-MM-dd"
    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    // Metoda pro vytvoření grafu s danými daty a formátovanými popisky
    private suspend fun createGraph(dataEntities: List<DataEntity>, formattedDateStrings: Array<String>) {
        // Vytvoření bodů pro sloupcový graf
        val dataPoints = dataEntities.mapIndexed { index, dataEntity ->
            DataPoint(index.toDouble() + 1, dataEntity.value)
        }.toTypedArray()

        // Vytvoření série pro sloupcový graf
        val series = BarGraphSeries(dataPoints)

        // Nastavení viditelné oblasti grafu na ose X
        val maxX = dataEntities.size.toDouble()
        graphView.viewport.setMinX(0.5)
        graphView.viewport.setMaxX(maxX + 0.5)
        graphView.viewport.isXAxisBoundsManual = true

        // Nastavení viditelné oblasti grafu na ose Y
        val maxY = dataEntities.maxByOrNull { it.value }?.value ?: 0.0
        graphView.viewport.setMinY(0.0)
        graphView.viewport.setMaxY(maxY)
        graphView.viewport.isYAxisBoundsManual = true

        // Odebrání předchozí série a přidání nové série do grafu
        graphView.removeAllSeries()
        graphView.addSeries(series)

        // Zajištění, že jsou alespoň dvě popisky na ose X před jejich nastavením
        if (formattedDateStrings.size >= 2) {
            // Přidání aktuálního data do pole pro popisky na ose X
            val currentDate = getCurrentDate()
            val dataEntity = controller.getDataByDate(currentDate)
            val formattedCurrentDate = dataEntity?.formattedDate ?: ""
            val allFormattedDates = formattedDateStrings + arrayOf(formattedCurrentDate)

            // Nastavení popisků na ose X pomocí allFormattedDates
            val staticLabelsFormatter = StaticLabelsFormatter(graphView)
            staticLabelsFormatter.setHorizontalLabels(allFormattedDates)
            graphView.gridLabelRenderer.labelFormatter = staticLabelsFormatter
            graphView.gridLabelRenderer.setHorizontalLabelsAngle(35)
            graphView.gridLabelRenderer.labelHorizontalHeight = 70
            graphView.gridLabelRenderer.numHorizontalLabels = allFormattedDates.size
            graphView.gridLabelRenderer.textSize = 30f
        }

        // Aktualizace TextView s hodnotami na ose Y
        updateYValuesTextView(dataEntities)
    }

    // Metoda pro aktualizaci TextView s hodnotami na ose Y
    private fun updateYValuesTextView(dataEntities: List<DataEntity>) {
        // Tato metoda není v kódu používána
        // Můžete ji odstranit, pokud není potřebná
    }

    // Metoda pro aktualizaci grafu ve specifickém vlákně
    private suspend fun updateGraph() {
        // Získání všech dat z databáze
        val dataEntities = database.dataDao().getAllData()
        // Získání formátovaných popisků pro osu X z databáze
        val formattedDateStrings = database.dataDao().getFormattedDates()

        // Přepnutí na hlavní vlákno pro aktualizaci UI
        withContext(Dispatchers.Main) {
            // Vytvoření grafu s daty a popisky
            createGraph(dataEntities, formattedDateStrings)
        }
    }

    // Přepsání metody pro zpětné tlačítko v akční liště
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
