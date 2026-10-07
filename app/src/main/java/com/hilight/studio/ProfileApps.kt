package com.hilight.studio

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import java.io.File

/**
 * Apps of the work and private profiles.
 *
 * This profile's package manager cannot see them, and the launcher service shows a private space
 * only to the default launcher. The shell or root identity can list every user's packages, together
 * with the APK each lives in; APKs are shared across users, so this app can then read the label and
 * icon straight out of the file.
 */
object ProfileApps {
    /** Run as shell/root: `user:<id>` then one `package:<apk>=<name>` line per third-party app. */
    const val SCRIPT =
        "for u in \$(pm list users | sed -n 's/.*UserInfo{\\([0-9]*\\):.*/\\1/p'); do " +
            "echo \"user:\$u\"; pm list packages -3 -f --user \$u; done"

    data class Entry(val userId: Int, val pkg: String, val apk: String)

    private const val PREFS = "hilight_profile_apps"

    /** `package:/data/app/~~x==/pkg-y==/base.apk=pkg` — the path may hold '=', the name never does. */
    internal fun parse(raw: String): List<Entry> {
        var user = -1
        val out = ArrayList<Entry>()
        for (line in raw.lineSequence().map { it.trim() }) {
            if (line.startsWith("user:")) {
                user = line.removePrefix("user:").toIntOrNull() ?: -1
            } else if (user >= 0 && line.startsWith("package:")) {
                val body = line.removePrefix("package:")
                val cut = body.lastIndexOf('=')
                if (cut <= 0) continue
                out += Entry(user, body.substring(cut + 1), body.substring(0, cut))
            }
        }
        return out
    }

    /** Entries of users other than [ownUser], first sighting of each package winning. */
    internal fun otherProfileEntries(raw: String, ownUser: Int): List<Entry> =
        parse(raw).filter { it.userId != ownUser }.distinctBy { it.pkg }

    private fun ownUserId() = android.os.Process.myUid() / 100_000

    /** Asks whichever privileged transport is up; null when none is, so callers fall back. */
    fun fetch(ctx: Context): List<Entry>? {
        val store = Store.get(ctx)
        val raw = (if (store.shizuku.state.value == ShizukuBackend.State.CONNECTED) {
            store.shizuku.listProfilePackages()
        } else null) ?: store.root.listProfilePackages() ?: return null
        val entries = otherProfileEntries(raw, ownUserId())
        remember(ctx, entries)
        return entries
    }

    /** Persisted so the notification listener can sample colors without a transport round-trip. */
    private fun remember(ctx: Context, entries: List<Entry>) {
        val edit = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear()
        entries.forEach { edit.putString(it.pkg, it.apk) }
        edit.apply()
    }

    fun apkFor(ctx: Context, pkg: String): String? =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(pkg, null)

    /** Application info with the APK attached, which is what makes label and icon loadable. */
    fun archiveInfo(ctx: Context, apk: String): ApplicationInfo? {
        if (!File(apk).canRead()) return null
        val info = ctx.packageManager.getPackageArchiveInfo(apk, 0)?.applicationInfo ?: return null
        info.sourceDir = apk
        info.publicSourceDir = apk
        return info
    }

    fun iconFor(ctx: Context, pkg: String): Drawable? {
        val info = archiveInfo(ctx, apkFor(ctx, pkg) ?: return null) ?: return null
        return runCatching { info.loadIcon(ctx.packageManager) }.getOrNull()
    }
}
