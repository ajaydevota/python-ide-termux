package com.sarvam.pythonide

import android.content.Context
import android.graphics.Typeface
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.dsl.languages
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import org.eclipse.tm4e.core.registry.IThemeSource

object EditorSetup {

    fun setup(context: Context, editor: CodeEditor) {
        editor.typefaceText = Typeface.MONOSPACE
        editor.setTextSize(14f)
        try {
            FileProviderRegistry.getInstance().addFileProvider(
                AssetsFileResolver(context.applicationContext.assets)
            )

            val themeRegistry = ThemeRegistry.getInstance()
            val themeName = "darcula"
            val themePath = "textmate/" + themeName + ".json"
            themeRegistry.loadTheme(
                ThemeModel(
                    IThemeSource.fromInputStream(
                        FileProviderRegistry.getInstance().tryGetInputStream(themePath),
                        themePath,
                        null
                    ),
                    themeName
                ).apply { isDark = true }
            )
            themeRegistry.setTheme(themeName)

            GrammarRegistry.getInstance().loadGrammars(
                languages {
                    language("python") {
                        grammar = "textmate/python/syntaxes/python.tmLanguage.json"
                        defaultScopeName()
                        languageConfiguration = "textmate/python/language-configuration.json"
                    }
                }
            )

            editor.colorScheme = TextMateColorScheme.create(themeRegistry)
            editor.setEditorLanguage(TextMateLanguage.create("source.python", true))
        } catch (e: Exception) {
            // TextMate setup failed; the editor still works with the default scheme.
        }
    }
}
