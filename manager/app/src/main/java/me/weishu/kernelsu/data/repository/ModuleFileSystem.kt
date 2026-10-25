package me.weishu.kernelsu.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.data.model.Module
import me.weishu.kernelsu.ui.util.awaitRootShell

/**
 * Reads the module directory directly instead of asking a daemon.
 *
 * The manager only needs to show what is already installed and flip the `disable` marker, so a
 * single root-shell command is enough and the app no longer depends on a bundled daemon binary.
 */
object ModuleFileSystem {

    private const val MODULE_DIR = "/data/adb/modules"

    /**
     * One record per line, tab separated, so a description containing a newline cannot shift the
     * columns. `tr` folds the remaining whitespace away before it reaches the parser.
     */
    private val LIST_SCRIPT = """
        for d in $MODULE_DIR/*; do
          [ -d "${'$'}d" ] || continue
          id=${'$'}{d##*/}
          prop="${'$'}d/module.prop"
          field() { sed -n "s/^${'$'}1=//p" "${'$'}prop" 2>/dev/null | head -n 1; }
          name=${'$'}(field name)
          version=${'$'}(field version)
          author=${'$'}(field author)
          code=${'$'}(field versionCode)
          desc=${'$'}(field description)
          {
            printf '%s\t%s\t%s\t%s\t%s\t%s\t' \
              "${'$'}id" "${'$'}name" "${'$'}version" "${'$'}author" "${'$'}code" "${'$'}desc"
            if [ -e "${'$'}d/disable" ]; then printf '0\t'; else printf '1\t'; fi
            if [ -e "${'$'}d/remove" ]; then printf '1\t'; else printf '0\t'; fi
            if [ -e "${'$'}d/action.sh" ]; then printf '1\t'; else printf '0\t'; fi
            if [ -d "${'$'}d/webroot" ]; then printf '1\t'; else printf '0\t'; fi
            if grep -qs '^metamodule=1' "${'$'}prop"; then printf '1\n'; else printf '0\n'; fi
          } | tr '\n' ' '
          echo
        done
    """.trimIndent()

    suspend fun list(): List<Module> = withContext(Dispatchers.IO) {
        val shell = awaitRootShell()
        val out = shell.newJob().add(LIST_SCRIPT).to(ArrayList(), null).exec().out
        out.mapNotNull(::parseLine)
    }

    /** Flips the `disable` marker of one module. */
    suspend fun setEnabled(id: String, enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        val quoted = "'" + id.replace("'", "'\"'\"'") + "'"
        val command = if (enabled) {
            "rm -f $MODULE_DIR/$quoted/disable"
        } else {
            "mkdir -p $MODULE_DIR/$quoted && touch $MODULE_DIR/$quoted/disable"
        }
        val shell = awaitRootShell()
        shell.newJob().add(command).to(ArrayList(), null).exec().isSuccess
    }

    private fun parseLine(line: String): Module? {
        val parts = line.trim().split('\t')
        if (parts.size < 11) return null
        val id = parts[0].ifBlank { return null }
        return Module(
            id = id,
            name = parts[1].trim(),
            version = parts[2].trim(),
            author = parts[3].ifBlank { "Unknown" },
            versionCode = parts[4].trim().toIntOrNull() ?: 0,
            description = parts[5].trim(),
            enabled = parts[6].trim() == "1",
            remove = parts[7].trim() == "1",
            hasActionScript = parts[8].trim() == "1",
            hasWebUi = parts[9].trim() == "1",
            metamodule = parts[10].trim() == "1",
            update = false,
            updateJson = "",
            actionIconPath = null,
            webUiIconPath = null,
        )
    }
}
