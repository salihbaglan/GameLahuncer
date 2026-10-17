package com.STechnology.gamelahuncer

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.STechnology.gamelahuncer.ui.theme.GameLahuncerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GameLahuncerTheme { GamesScreen(lifecycle) } }
    }
}

private data class InstalledGame(val packageName: String, val name: String, val icon: ImageBitmap)

@Suppress("DEPRECATION")
private fun loadGames(context: android.content.Context): List<InstalledGame> {
    val pm = context.packageManager
    val mask = systemIconMask(context)
    val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val collator = Collator.getInstance()
    val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(launcher, PackageManager.ResolveInfoFlags.of(0))
    } else {
        pm.queryIntentActivities(launcher, 0)
    }
    return activities
        .map { it.activityInfo.applicationInfo }
        .distinctBy { it.packageName }
        .filter {
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && it.category == ApplicationInfo.CATEGORY_GAME) ||
                (it.flags and ApplicationInfo.FLAG_IS_GAME) != 0
        }
        .mapNotNull { app ->
            try {
                InstalledGame(app.packageName, pm.getApplicationLabel(app).toString(),
                    maskedIcon(pm.getApplicationIcon(app), mask))
            } catch (_: PackageManager.NameNotFoundException) { null }
        }
        .sortedWith { a, b -> collator.compare(a.name, b.name) }
}

// Framework masks use a 100 x 100 viewport; fall back to a rounded square.
private fun systemIconMask(context: android.content.Context): android.graphics.Path? {
    val resources = context.resources
    val id = resources.getIdentifier("config_icon_mask", "string", "android")
    if (id == 0) return null
    return try {
        androidx.core.graphics.PathParser.createPathFromPathData(resources.getString(id))
            ?.takeUnless { it.isEmpty }
    } catch (_: android.content.res.Resources.NotFoundException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun maskedIcon(drawable: android.graphics.drawable.Drawable, mask: android.graphics.Path?): ImageBitmap {
    val size = 144
    val original = drawable.toBitmap(size, size)

    val output = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val path = mask?.let { android.graphics.Path(it) } ?: android.graphics.Path().apply {
        addRoundRect(0f, 0f, 100f, 100f, 22f, 22f, android.graphics.Path.Direction.CW)
    }
    path.transform(android.graphics.Matrix().apply { setScale(size / 100f, size / 100f) })
    val canvas = android.graphics.Canvas(output)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        shader = android.graphics.BitmapShader(original, android.graphics.Shader.TileMode.CLAMP,
            android.graphics.Shader.TileMode.CLAMP)
    }
    canvas.drawPath(path, paint)
    return output.asImageBitmap()
}
@Composable
private fun GamesScreen(lifecycle: Lifecycle) {
    val context = LocalContext.current
    var games by remember { mutableStateOf<List<InstalledGame>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refresh by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(refresh) {
        loading = true
        try {
            games = withContext(Dispatchers.IO) { loadGames(context) }
        } catch (error: SecurityException) {
            android.util.Log.e("GameLauncher", "Unable to load games", error)
        } finally { loading = false }
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(88.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(24.dp)
            ) {
                items(games, key = { it.packageName }) { game ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                            .clickable(onClickLabel = stringResource(R.string.open_game, game.name)) {
                                try {
                                    val intent = context.packageManager.getLaunchIntentForPackage(game.packageName)
                                    if (intent != null) context.startActivity(intent)
                                } catch (error: android.content.ActivityNotFoundException) {
                                    android.util.Log.w("GameLauncher", "Game no longer installed", error)
                                    refresh++
                                } catch (error: SecurityException) {
                                    android.util.Log.w("GameLauncher", "Game launch blocked", error)
                                }
                            }
                    ) {
                        Spacer(Modifier.height(12.dp))
                        Image(game.icon, contentDescription = null,
                            modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = game.name,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2,
                            minLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}




