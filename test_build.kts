import java.io.File
import com.aistudio.xide.core.build.*
import com.aistudio.xide.core.diagnostics.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// We can't really run a Kotlin script that imports from the app module easily unless we use an Android instrumentation test or a local unit test.
