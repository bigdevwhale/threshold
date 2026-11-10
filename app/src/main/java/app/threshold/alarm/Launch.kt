package app.threshold.alarm

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import app.threshold.data.Kind
import app.threshold.data.Step

/** Turns a step of the morning into the intent that opens it. */
object Launch {
    /** Apps that open their own links better than a browser does. */
    private val hosts = listOf(
        "youtube.com" to "com.google.android.youtube",
        "youtu.be" to "com.google.android.youtube",
        "music.yandex" to "ru.yandex.music",
        "kinopoisk.ru" to "ru.kinopoisk",
        "m.youtube.com" to "com.google.android.youtube",
        "open.spotify.com" to "com.spotify.music",
        "music.youtube.com" to "com.google.android.apps.youtube.music",
        "vk.com" to "com.vkontakte.android",
        "rutube.ru" to "ru.rutube.app",
    )

    fun installed(ctx: Context, pkg: String): Boolean =
        pkg.isNotEmpty() && runCatching { ctx.packageManager.getPackageInfo(pkg, 0); true }.getOrDefault(false)

    /** The app a link would best open in, if it's installed. */
    fun appFor(ctx: Context, url: String): String? {
        if (url.startsWith("spotify:")) return "com.spotify.music".takeIf { installed(ctx, it) }
        val host = runCatching { Uri.parse(url).host.orEmpty() + Uri.parse(url).path.orEmpty() }.getOrDefault("")
        return hosts.filter { (h, _) -> h in host }.sortedByDescending { it.first.length }.map { it.second }.firstOrNull { installed(ctx, it) }
    }

    /**
     * Rewrites a link into the form its app opens directly and, for music, starts playing at once (all checked on a
     * phone): Kinopoisk takes www.kinopoisk.ru/film/… but not hd.kinopoisk.ru/film/…; a Spotify playlist or album only
     * opens from its web link but plays as spotify:playlist:<id>:play (a track link plays as it is); Yandex Music
     * plays an album or playlist given ?play=true.
     */
    fun normalize(url: String): String {
        val u = url.trim()
        Regex("""https?://hd\.kinopoisk\.ru/(film|series)/(\d+)""").find(u)?.let { m -> return "https://www.kinopoisk.ru/${m.groupValues[1]}/${m.groupValues[2]}/" }
        Regex("""https?://open\.spotify\.com/(?:intl-[a-z]+/)?(playlist|album|artist|show|episode)/([A-Za-z0-9]+)""").find(u)?.let { m ->
            return "spotify:${m.groupValues[1]}:${m.groupValues[2]}:play"
        }
        if (Regex("""https?://music\.yandex\.[a-z]+/(album|users/[^/]+/playlists|playlists?|artist|chart)""").containsMatchIn(u) && !u.contains("play=")) {
            val clean = u.substringBefore('#')
            return clean + (if ('?' in clean) "&" else "?") + "play=true"
        }
        return u
    }

    /** Where a link will open: in its own app, in the browser although the app is installed, or just in the browser. */
    sealed interface Where {
        data class App(val pkg: String) : Where
        data class NotTaken(val pkg: String) : Where
        data object Web : Where
    }

    fun where(ctx: Context, url: String): Where {
        val u = normalize(url)
        val uri = runCatching { Uri.parse(u) }.getOrNull() ?: return Where.Web
        val pkg = appFor(ctx, u) ?: return Where.Web
        val i = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE).setPackage(pkg)
        return if (i.resolveActivity(ctx.packageManager) != null) Where.App(pkg) else Where.NotTaken(pkg)
    }

    private fun home(uri: Uri) = uri.isHierarchical && (uri.path.isNullOrEmpty() || uri.path == "/")

    fun intent(ctx: Context, step: Step): Intent? = when (step.kind) {
        Kind.RADIO -> null
        // A file picked from the phone: the app keeps a lasting right to read it and lends it to the player.
        Kind.FILE -> runCatching { Uri.parse(step.url) }.getOrNull()?.takeIf { step.url.isNotBlank() }?.let { uri ->
            val type = runCatching { ctx.contentResolver.getType(uri) }.getOrNull() ?: step.pkg.ifBlank { "*/*" }
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, type).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        Kind.APP -> ctx.packageManager.getLaunchIntentForPackage(step.pkg)
        Kind.LINK -> {
            val uri = runCatching { Uri.parse(normalize(step.url)) }.getOrNull()
            if (uri == null || step.url.isBlank()) null
            else {
                val i = Intent(Intent.ACTION_VIEW, uri)
                val pkg = step.pkg.takeIf { installed(ctx, it) } ?: appFor(ctx, step.url)
                when {
                    pkg == null -> i
                    i.setPackage(pkg).resolveActivity(ctx.packageManager) != null -> i
                    // Just the site's front page: the app's own front page is better.
                    home(uri) -> ctx.packageManager.getLaunchIntentForPackage(pkg) ?: i.setPackage(null)
                    // The app is there but doesn't take this exact link: the exact page in the browser beats the app's home.
                    else -> i.setPackage(null)
                }
            }
        }
    }?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun open(ctx: Context, step: Step): Boolean {
        val i = intent(ctx, step) ?: return false
        return runCatching { ctx.startActivity(i); true }.getOrElse {
            // The chosen app is gone: let Android find another one for the link.
            if (step.kind == Kind.LINK) runCatching { ctx.startActivity(i.setPackage(null)); true }.getOrDefault(false) else false
        }
    }

    /** Launchable apps for the picker. */
    data class App(val pkg: String, val label: String)

    /** The installed apps change rarely; the list is kept for the session once read (reading labels is slow). */
    @Volatile private var cached: List<App>? = null

    fun apps(ctx: Context): List<App> = cached ?: readApps(ctx).also { cached = it }

    private fun readApps(ctx: Context): List<App> {
        val pm = ctx.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val list = if (android.os.Build.VERSION.SDK_INT >= 33) pm.queryIntentActivities(main, PackageManager.ResolveInfoFlags.of(0)) else pm.queryIntentActivities(main, 0)
        return list.map { App(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.pkg != ctx.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }
}
