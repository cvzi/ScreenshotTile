package com.github.cvzi.screenshottile.activities


import android.annotation.SuppressLint
import android.app.appfunctions.AppFunctionException
import android.app.appfunctions.AppFunctionManager
import android.app.appfunctions.ExecuteAppFunctionRequest
import android.app.appfunctions.ExecuteAppFunctionResponse
import android.app.appsearch.AppSearchManager
import android.app.appsearch.GenericDocument
import android.app.appsearch.SearchSpec
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.cvzi.screenshottile.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@SuppressLint("SetTextI18n")
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
class AppfunctionsTestActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var resultView: TextView
    private lateinit var btnRefresh: Button
    private lateinit var btnGetCapture: Button

    private var lastRequestId: String? = null
    private var getCaptureFn: FnMeta? = null
    private val mainExec = Executors.newSingleThreadExecutor()

    data class FnMeta(
        val functionId: String,
        val title: String,
        val packageName: String?,
        val type: String?,
        val raw: GenericDocument
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_local_test)
        listView = findViewById(R.id.listFunctions)
        resultView = findViewById(R.id.txtResult)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnGetCapture = findViewById(R.id.btnGetCapture)

        btnRefresh.setOnClickListener { loadFunctions() }
        btnGetCapture.isEnabled = false
        btnGetCapture.setOnClickListener { executeGetCapture() }

        loadFunctions()
    }

    private fun loadFunctions() {
        Log.v("AppFunctionTest", "Loading functions...")
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val fns = queryFunctions()
                Log.v("AppFunctionTest", "fns = $fns")

                // Remember the function from the result for the second button action
                getCaptureFn = fns.firstOrNull { it.functionId.endsWith("#getCaptureResult") }


                val adapter = ArrayAdapter(
                    this@AppfunctionsTestActivity,
                    android.R.layout.simple_list_item_2,
                    android.R.id.text1,
                    fns.map { "${it.title} (${it.type})" }
                )
                listView.adapter = adapter
                listView.setOnItemClickListener { _, _, pos, _ -> execute(fns[pos]) }
                resultView.text = "Found ${fns.size} function(s). Tap to execute."
            } catch (t: Throwable) {
                resultView.text = "Discovery error: $t"
                Log.e("AppFunctionTest", "Discovery failed", t)
            }
        }
    }


    private suspend fun queryFunctions(): List<FnMeta> {
        Log.v("AppFunctionTest", "Querying functions...")

        val appSearchMgr = getSystemService(AppSearchManager::class.java)

        val session = suspendCancellableCoroutine { cont ->
            appSearchMgr.createGlobalSearchSession(mainExec) { res ->
                Log.v("AppFunctionTest", "createGlobalSearchSession: $res")
                if (res.isSuccess) cont.resume(res.resultValue!!)
                else cont.resumeWithException(IllegalStateException("GlobalSearchSession: $res"))
            }
        }

        val spec = SearchSpec.Builder()
            .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
            .addFilterSchemas(
                "AppFunctionRuntimeMetadata-$packageName",
                "AppFunctionStaticMetadata-$packageName"
            )
            .build()

        val results = session.search("", spec)

        val page = suspendCancellableCoroutine { cont ->
            results.getNextPage(mainExec) { pageRes ->
                if (pageRes.isSuccess) cont.resume(pageRes.resultValue!!)
                else cont.resumeWithException(IllegalStateException("getNextPage: $pageRes"))
            }
        }

        Log.v("AppFunctionTest", "Got ${page.size} results")

        return page.map { r ->
            Log.v("AppFunctionTest", "Result: ${r.genericDocument}")
            val doc = r.genericDocument
            val functionId = doc.getPropertyString("functionId")
                ?: doc.id.substringAfter('/')
            val title = doc.getPropertyString("displayName")
                ?: doc.getPropertyString("name")
                ?: doc.id
            val pkg = doc.getPropertyString("packageName") ?: doc.namespace
            val type = doc.getPropertyString("namespace") ?: doc.schemaType
            FnMeta(
                functionId = functionId,
                title = title,
                packageName = pkg,
                type = type,
                raw = doc
            )
        }
    }

    private fun execute(meta: FnMeta) {
        val mgr = getSystemService(AppFunctionManager::class.java)

        // Build request: target package + functionId (NOT doc.id)
        val req = ExecuteAppFunctionRequest.Builder(
            meta.packageName ?: packageName,
            meta.functionId
        )
            // Optional set parameters
            // .setParameters(...)

            .build()

        Log.v("AppFunctionTest", "Executing ${meta.title} (id=${meta.functionId})…")
        resultView.text = "Executing ${meta.title} (id=${meta.functionId})…"

        val cancel = CancellationSignal()
        val exec: Executor = mainExecutor

        mgr.executeAppFunction(
            req,
            exec,
            cancel,
            object : OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException> {
                override fun onResult(res: ExecuteAppFunctionResponse) {
                    Log.v("AppFunctionTest", "AppFunction result: $res")
                    val payload = res.resultDocument
                    Log.v("AppFunctionTest", "AppFunction result: $payload")

                    // properties["androidAppfunctionsReturnValue"] -> array of GenericDocument
                    val returnDocs = try {
                        payload.getPropertyDocumentArray("androidAppfunctionsReturnValue")
                    } catch (_: Throwable) {
                        emptyArray<GenericDocument>()
                    }

                    val startResp = returnDocs?.firstOrNull()
                    val requestId = startResp?.getPropertyString("requestId")

                    if (requestId != null) {
                        lastRequestId = requestId
                        btnGetCapture.isEnabled = (getCaptureFn != null)
                    }

                    runOnUiThread {
                        resultView.text = buildString {
                            append("Success!\n")
                            append(payload.schemaType).append('\n')
                            append(payload.propertyNames.joinToString()).append('\n')
                            append(payload.toString())
                        }
                    }

                }

                override fun onError(error: AppFunctionException) {
                    Log.e(
                        "AppFunctionTest",
                        "AppFunction failed. code=${error.errorCode}, message=${error.message}, extras=${error.extras}",
                        error
                    )
                    runOnUiThread {
                        resultView.text = "Error (${error.errorCode}): ${error.message}"
                    }
                }
            }
        )
    }

    private fun executeGetCapture() {
        val fn = getCaptureFn
        val reqId = lastRequestId
        if (fn == null || reqId.isNullOrEmpty()) {
            Toast.makeText(this, "No requestId yet", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("AppFunctionTest", "Executing getCaptureResult for $reqId")

        val mgr = getSystemService(AppFunctionManager::class.java)

        // Build parameters
        val params = GenericDocument.Builder<GenericDocument.Builder<*>>(
            packageName,
            "params",
            "com.github.cvzi.screenshottile.functions.GetCaptureRequest"
        )
            .setPropertyString("requestId", reqId)
            .build()

        val req = ExecuteAppFunctionRequest.Builder(
            fn.packageName ?: packageName,
            fn.functionId
        )
            .setParameters(params)
            .build()

        val cancel = CancellationSignal()
        val exec = mainExecutor

        resultView.text = "Querying result for $reqId…"

        mgr.executeAppFunction(
            req,
            exec,
            cancel,
            object : OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException> {
                override fun onResult(res: ExecuteAppFunctionResponse) {
                    val payload = res.resultDocument
                    val docs = try {
                        payload.getPropertyDocumentArray("androidAppfunctionsReturnValue")
                    } catch (_: Throwable) {
                        emptyArray<GenericDocument>()
                    }

                    val result = docs?.firstOrNull()
                    val status = result?.getPropertyString("status")
                    val contentUri = result?.getPropertyString("contentUri")
                    val w = result?.getPropertyLong("width")?.toString()
                    val h = result?.getPropertyLong("height")?.toString()

                    runOnUiThread {
                        resultView.text = buildString {
                            append("getCaptureResult → status=").append(status).append('\n')
                            if (!contentUri.isNullOrEmpty()) {
                                append("uri=").append(contentUri).append('\n')
                            }
                            if (w != null && h != null) {
                                append("size=").append(w).append("x").append(h).append('\n')
                            }
                            append("\nRaw: ").append(result ?: payload)
                        }
                    }
                }

                override fun onError(error: AppFunctionException) {
                    Log.e(
                        "AppFunctionTest",
                        "getCaptureResult failed code=${error.errorCode}, msg=${error.message}, extras=${error.extras}",
                        error
                    )
                    runOnUiThread {
                        resultView.text = "Error (${error.errorCode}): ${error.message}"
                    }
                }
            }
        )
    }


}