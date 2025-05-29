// File: core/src/com/unciv/ui/screens/mainmenuscreen/MainMenuScreen.kt (Modified)
package com.unciv.ui.screens.mainmenuscreen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Dialog // Ensure Dialog is imported
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.Constants
import com.unciv.GUI
import com.unciv.UncivGame
import com.unciv.interfaces.IPlatformBridge // << ADD IMPORT for the new interface
import com.unciv.interfaces.HelpshiftOptions // << ADD IMPORT for the type alias
import com.unciv.logic.GameInfo
import com.unciv.logic.GameStarter
import com.unciv.logic.HolidayDates
import com.unciv.logic.UncivShowableException
import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.MapShape
import com.unciv.logic.map.MapSize
import com.unciv.logic.map.MapType
import com.unciv.logic.map.mapgenerator.MapGenerator
import com.unciv.models.metadata.BaseRuleset
import com.unciv.models.metadata.GameSetupInfo
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.RulesetCache
import com.unciv.models.tilesets.TileSetCache
import com.unciv.models.translations.tr // Ensure tr is available/imported
import com.unciv.ui.audio.SoundPlayer
import com.unciv.ui.components.UncivTooltip.Companion.addTooltip
import com.unciv.ui.components.extensions.center
import com.unciv.ui.components.extensions.surroundWithCircle
import com.unciv.ui.components.extensions.surroundWithThinCircle
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.components.input.KeyShortcutDispatcherVeto
import com.unciv.ui.components.input.KeyboardBinding
import com.unciv.ui.components.input.keyShortcuts
import com.unciv.ui.components.input.onActivation
import com.unciv.ui.components.input.onLongPress
import com.unciv.ui.components.tilegroups.TileGroupMap
import com.unciv.ui.components.widgets.AutoScrollPane
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.images.padTopDescent
import com.unciv.ui.popups.Popup
import com.unciv.ui.popups.ToastPopup
import com.unciv.ui.popups.closeAllPopups
import com.unciv.ui.popups.hasOpenPopups
import com.unciv.ui.popups.popups // If not used directly
import com.unciv.ui.screens.basescreen.BaseScreen
import com.unciv.ui.screens.basescreen.RecreateOnResize
import com.unciv.ui.screens.mainmenuscreen.EasterEggRulesets.modifyForEasterEgg
import com.unciv.ui.screens.mapeditorscreen.EditorMapHolder
import com.unciv.ui.screens.mapeditorscreen.MapEditorScreen
import com.unciv.ui.screens.modmanager.ModManagementScreen
import com.unciv.ui.screens.multiplayerscreens.MultiplayerScreen
import com.unciv.ui.screens.newgamescreen.NewGameScreen
import com.unciv.ui.screens.savescreens.LoadGameScreen
import com.unciv.ui.screens.savescreens.QuickSave
import com.unciv.ui.screens.worldscreen.BackgroundActor
import com.unciv.ui.screens.worldscreen.WorldScreen
import com.unciv.ui.screens.worldscreen.mainmenu.WorldScreenMenuPopup
import com.unciv.utils.Concurrency
import com.unciv.utils.launchOnGLThread
import kotlinx.coroutines.Job
import kotlin.collections.HashMap // Using Kotlin's HashMap explicitly
import kotlin.math.min


class MainMenuScreen: BaseScreen(), RecreateOnResize {
    // ... (backgroundStack, singleColumn, backgroundMapRuleset, etc. as in your file) ...
    private val backgroundStack = Stack()
    private val singleColumn = isCrampedPortrait()

    private val backgroundMapRuleset: Ruleset
    private var easterEggRuleset: Ruleset? = null

    private var backgroundMapGenerationJob: Job? = null
    private var backgroundMapExists = false

    companion object {
        const val mapFadeTime = 1.3f
        const val mapFirstFadeTime = 0.3f
        const val mapReplaceDelay = 20f
    }

    private fun getMenuButton(
        text: String,
        icon: String,
        binding: KeyboardBinding,
        function: () -> Unit
    ): Table {
        // ... (this method remains unchanged from your provided code) ...
        val table = Table().pad(15f, 30f, 15f, 30f)
        table.background = skinStrings.getUiBackground(
            "MainMenuScreen/MenuButton",
            skinStrings.roundedEdgeRectangleShape,
            skinStrings.skinConfig.baseColor
        )
        table.add(ImageGetter.getImage(icon)).size(50f).padRight(20f)
        table.add(text.toLabel(fontSize = 30, alignment = Align.left)).expand().left().minWidth(200f)
            .padTopDescent()

        table.touchable = Touchable.enabled
        table.onActivation(binding = binding) {
            stopBackgroundMapGeneration()
            function()
        }

        table.pack()
        return table
    }

    // << NEW METHOD to create the Helpshift FAQ Button >>
    private fun getHelpshiftFaqButton(): Table {
        val faqButtonTable = Table().pad(15f, 30f, 15f, 30f) // Consistent padding
        faqButtonTable.background = skinStrings.getUiBackground(
            "MainMenuScreen/MenuButton", // Reuse existing style
            skinStrings.roundedEdgeRectangleShape,
            skinStrings.skinConfig.baseColor
        )
        val faqLabel = "Show FAQs".tr().toLabel(fontSize = 30, alignment = Align.center)
        faqButtonTable.add(faqLabel).expandX().center().padTopDescent()

        faqButtonTable.touchable = Touchable.enabled
        faqButtonTable.onActivation {
            val bridge = UncivGame.Current.platformBridge // Get the bridge from UncivGame

            if (bridge.isHelpshiftFeatureAvailable()) {
                val configMap: HelpshiftOptions = HashMap<String, Any>() // Explicitly use HashMap or emptyMap()
                // Example: Add custom metadata or tags if needed for context
                // val metadata = hashMapOf("source" to "main_menu_faqs", "game_version" to UncivGame.Current.version.text)
                // configMap["customMetadata"] = metadata
                // val tagsArray = arrayOf("main_menu", "general_support")
                // configMap["tags"] = tagsArray

                Gdx.app.log("MainMenuScreen", "Showing Helpshift FAQs via PlatformBridge")
                bridge.showHelpshiftFAQs(configMap) // Call the interface method
            } else {
                Gdx.app.log("MainMenuScreen", "Helpshift not available (via PlatformBridge). Showing fallback.")
                val fallbackDialog = Dialog("Support Unavailable".tr(), skin)
                // IMPORTANT: Replace [YOUR_UNCIV_SUPPORT_EMAIL_OR_FORUM_URL] with your actual support details.
                val fallbackMessage = "In-app help is currently unavailable on this platform or requires a newer Android version. For assistance, please visit our support page or contact us at [YOUR_UNCIV_SUPPORT_EMAIL_OR_FORUM_URL]."
                fallbackDialog.text(fallbackMessage.tr())
                fallbackDialog.button("OK".tr())
                fallbackDialog.show(stage) // 'stage' is available from BaseScreen
            }
        }
        faqButtonTable.pack() // Ensure the button itself is packed
        return faqButtonTable
    }

    init {
        SoundPlayer.initializeForMainMenu()

        val background = skinStrings.getUiBackground("MainMenuScreen/Background", tintColor = clearColor)
        backgroundStack.add(BackgroundActor(background, Align.center))
        stage.addActor(backgroundStack)
        backgroundStack.setFillParent(true)

        val baseRuleset = RulesetCache.getVanillaRuleset()
        ImageGetter.setNewRuleset(baseRuleset)

        if (game.settings.enableEasterEggs) {
            val holiday = HolidayDates.getHolidayByDate()
            if (holiday != null)
                EasterEggFloatingArt(stage, holiday.name)
            val easterEggMod = EasterEggRulesets.getTodayEasterEggRuleset()
            if (easterEggMod != null)
                easterEggRuleset = RulesetCache.getComplexRuleset(baseRuleset, listOf(easterEggMod))
        }
        backgroundMapRuleset = easterEggRuleset ?: baseRuleset

        if (game.settings.tileSet in TileSetCache)
            startBackgroundMapGeneration()

        // << NEW: Master layout table >>
        val mainLayoutTable = Table()
        mainLayoutTable.setFillParent(true)
        stage.addActor(mainLayoutTable) // Add it on top of the background

        // << ADD FAQ Button to the top row of mainLayoutTable >>
        val helpshiftFaqButton = getHelpshiftFaqButton()
        mainLayoutTable.add(helpshiftFaqButton)
            .center()       // Center the button table within its cell
            .padBottom(20f) // Add some space below it
            .row()          // Move to the next row in mainLayoutTable

        // Existing button column setup (your logic for column1 and column2 is fine)
        val column1 = Table().apply { defaults().pad(10f).fillX() }
        val column2 = if (singleColumn) column1 else Table().apply { defaults().pad(10f).fillX() }

        if (game.files.autosaves.autosaveExists()) {
            val resumeTable = getMenuButton("Resume","OtherIcons/Resume", KeyboardBinding.Resume)
            { resumeGame() }
            column1.add(resumeTable).row()
        }

        val quickstartTable = getMenuButton("Quickstart", "OtherIcons/Quickstart", KeyboardBinding.Quickstart)
        { quickstartNewGame() }
        column1.add(quickstartTable).row()

        // Assuming NewGameScreen(), LoadGameScreen() etc. are parameterless constructors
        val newGameButton = getMenuButton("Start new game", "OtherIcons/New", KeyboardBinding.StartNewGame)
        { game.pushScreen(NewGameScreen()) }
        column1.add(newGameButton).row()

        val loadGameTable = getMenuButton("Load game", "OtherIcons/Load", KeyboardBinding.MainMenuLoad)
        { game.pushScreen(LoadGameScreen()) }
        column1.add(loadGameTable).row()

        // If singleColumn is true, column2 is an alias for column1, so these are added to column1. Correct.
        val multiplayerTable = getMenuButton("Multiplayer", "OtherIcons/Multiplayer", KeyboardBinding.Multiplayer)
        { game.pushScreen(MultiplayerScreen()) }
        column2.add(multiplayerTable).row()

        val mapEditorScreenTable = getMenuButton("Map editor", "OtherIcons/MapEditor", KeyboardBinding.MapEditor)
        { game.pushScreen(MapEditorScreen()) }
        column2.add(mapEditorScreenTable).row()

        val modsTable = getMenuButton("Mods", "OtherIcons/Mods", KeyboardBinding.ModManager)
        { game.pushScreen(ModManagementScreen()) }
        column2.add(modsTable).row()

        val optionsTable = getMenuButton("Options", "OtherIcons/Options", KeyboardBinding.MainMenuOptions)
        { openOptionsPopup() }
        optionsTable.onLongPress { openOptionsPopup(withDebug = true) }
        column2.add(optionsTable).row()

        // This 'buttonColumnsHolderTable' (renamed from your 'table' for clarity) holds column1 and potentially column2
        val buttonColumnsHolderTable = Table().apply { defaults().pad(10f) }
        buttonColumnsHolderTable.add(column1)
        if (!singleColumn) {
            buttonColumnsHolderTable.add(column2)
        }
        buttonColumnsHolderTable.pack() // Keep this from your original code

        val scrollPane = AutoScrollPane(buttonColumnsHolderTable)
        // scrollPane.setFillParent(true) // This is NO LONGER set on scrollPane directly

        // Center the buttonColumnsHolderTable within the scrollPane view
        buttonColumnsHolderTable.center(scrollPane) // Keep this from your original code

        // << ADD scrollPane to the second row of mainLayoutTable >>
        mainLayoutTable.add(scrollPane)
            .expand() // Expand to take available vertical and horizontal space in its cell
            .fill()   // Fill that space
            .row()    // End of row for scrollPane


        // The rest of your UI elements (Civilopedia, social buttons, version)
        // are added directly to stage, so their positioning logic should remain unaffected.
        globalShortcuts.add(KeyboardBinding.QuitMainMenu) {
            if (hasOpenPopups()) {
                closeAllPopups()
                return@add
            }
            game.popScreen()
        }

        val civilopediaButton = "?".toLabel(fontSize = 48)
            .apply { setAlignment(Align.center) }
            .surroundWithCircle(60f, color = skinStrings.skinConfig.baseColor)
            .apply { actor.y -= 2.5f }
            .surroundWithCircle(64f, resizeActor = false)
        civilopediaButton.touchable = Touchable.enabled
        civilopediaButton.onActivation { openCivilopedia() }
        civilopediaButton.keyShortcuts.add(KeyboardBinding.Civilopedia)
        civilopediaButton.addTooltip(KeyboardBinding.Civilopedia, 30f)
        civilopediaButton.setPosition(30f, 30f)
        stage.addActor(civilopediaButton)

        val rightSideButtons = Table().apply { defaults().pad(10f) }
        val discordButton = ImageGetter.getImage("OtherIcons/Discord")
            .surroundWithCircle(60f, color = skinStrings.skinConfig.baseColor)
            .surroundWithThinCircle(Color.WHITE)
            .onActivation { Gdx.net.openURI("https://discord.gg/bjrB4Xw") }
        rightSideButtons.add(discordButton)

        val githubButton = ImageGetter.getImage("OtherIcons/Github")
            .surroundWithCircle(60f, color = skinStrings.skinConfig.baseColor)
            .surroundWithThinCircle(Color.WHITE)
            .onActivation { Gdx.net.openURI("https://github.com/yairm210/Unciv") }
        rightSideButtons.add(githubButton)

        rightSideButtons.pack()
        rightSideButtons.setPosition(stage.width - 30, 30f, Align.bottomRight)
        stage.addActor(rightSideButtons)

        val versionLabel = "{Version} ${UncivGame.VERSION.text}".toLabel()
        versionLabel.setAlignment(Align.center)
        val versionTable = Table()
        versionTable.background = skinStrings.getUiBackground("MainMenuScreen/Version",
            skinStrings.roundedEdgeRectangleShape, Color.DARK_GRAY.cpy().apply { a=0.7f })
        versionTable.add(versionLabel)
        versionTable.pack()
        versionTable.setPosition(stage.width/2, 10f, Align.bottom)
        stage.addActor(versionTable)
    }

    // ... (startBackgroundMapGeneration, stopBackgroundMapGeneration, resumeGame, quickstartNewGame methods as in your file) ...
    private fun startBackgroundMapGeneration() {
        stopBackgroundMapGeneration()
        backgroundMapGenerationJob = Concurrency.run("ShowMapBackground") {
            var scale = 1f
            var mapWidth = stage.width / TileGroupMap.groupHorizontalAdvance
            var mapHeight = stage.height / TileGroupMap.groupSize
            if (mapWidth * mapHeight > 3000f) {
                scale = mapWidth * mapHeight / 3000f
                mapWidth /= scale
                mapHeight /= scale
                scale = min(scale, 20f)
            }

            val newMap = MapGenerator(backgroundMapRuleset, this)
                .generateMap(MapParameters().apply {
                    shape = MapShape.rectangular
                    mapSize = MapSize.Small
                    type = MapType.pangaea
                    temperatureintensity = .7f
                    waterThreshold = -0.1f
                    modifyForEasterEgg()
                })

            launchOnGLThread {
                ImageGetter.setNewRuleset(backgroundMapRuleset, ignoreIfModsAreEqual = true)
                val mapHolder = EditorMapHolder(
                    this@MainMenuScreen,
                    newMap
                ) {}
                mapHolder.setScale(scale)
                mapHolder.color = mapHolder.color.cpy()
                mapHolder.color.a = 0f
                backgroundStack.add(mapHolder)

                if (backgroundMapExists) {
                    mapHolder.addAction(Actions.sequence(
                        Actions.fadeIn(mapFadeTime),
                        Actions.run { backgroundStack.removeActorAt(1, false) }
                    ))
                } else {
                    backgroundMapExists = true
                    mapHolder.addAction(Actions.fadeIn(mapFirstFadeTime))
                }
            }
        }.apply {
            invokeOnCompletion {
                backgroundMapGenerationJob = null
                backgroundStack.addAction(Actions.sequence(
                    Actions.delay(mapReplaceDelay),
                    Actions.run { startBackgroundMapGeneration() }
                ))
            }
        }
    }

    private fun stopBackgroundMapGeneration() {
        backgroundStack.clearActions()
        val currentJob = backgroundMapGenerationJob
            ?: return
        backgroundMapGenerationJob = null
        if (currentJob.isCancelled) return
        currentJob.cancel()
    }

    private fun resumeGame() {
        if (GUI.isWorldLoaded()) {
            val currentTileSet = GUI.getMap().currentTileSetStrings
            val currentGameSetting = GUI.getSettings()
            if (currentTileSet.tileSetName != currentGameSetting.tileSet ||
                currentTileSet.unitSetName != currentGameSetting.unitSet) {
                game.removeScreensOfType(WorldScreen::class)
                QuickSave.autoLoadGame(this)
            } else {
                GUI.resetToWorldScreen()
                GUI.getWorldScreen().popups.filterIsInstance<WorldScreenMenuPopup>().forEach(Popup::close)
            }
        } else {
            QuickSave.autoLoadGame(this)
        }
    }

    private fun quickstartNewGame() {
        ToastPopup(Constants.working, this)
        val errorText = "Cannot start game with the default new game parameters!"
        Concurrency.run("QuickStart") {
            val newGame: GameInfo
            try {
                val gameInfo = GameSetupInfo.fromSettings("Chieftain")
                if (gameInfo.gameParameters.victoryTypes.isEmpty()) {
                    val ruleSet = RulesetCache.getComplexRuleset(gameInfo.gameParameters)
                    gameInfo.gameParameters.victoryTypes.addAll(ruleSet.victories.keys)
                }
                newGame = GameStarter.startNewGame(gameInfo)

            } catch (notAPlayer: UncivShowableException) {
                val (message) = LoadGameScreen.getLoadExceptionMessage(notAPlayer)
                launchOnGLThread { ToastPopup(message, this@MainMenuScreen) }
                return@run
            } catch (_: Exception) {
                launchOnGLThread { ToastPopup(errorText, this@MainMenuScreen) }
                return@run
            }

            try {
                game.loadGame(newGame)
            } catch (_: OutOfMemoryError) {
                launchOnGLThread {
                    ToastPopup("Not enough memory on phone to load game!", this@MainMenuScreen)
                }
            } catch (notAPlayer: UncivShowableException) {
                val (message) = LoadGameScreen.getLoadExceptionMessage(notAPlayer)
                launchOnGLThread {
                    ToastPopup(message, this@MainMenuScreen)
                }
            } catch (_: Exception) {
                launchOnGLThread {
                    ToastPopup(errorText, this@MainMenuScreen)
                }
            }
        }
    }

    override fun getCivilopediaRuleset(): Ruleset {
        val rulesetParameters = game.settings.lastGameSetup?.gameParameters
        if (rulesetParameters != null) return RulesetCache.getComplexRuleset(rulesetParameters)
        return RulesetCache[BaseRuleset.Civ_V_GnK.fullName]
            ?: throw IllegalStateException("No ruleset found")
    }

    override fun openCivilopedia(link: String) {
        stopBackgroundMapGeneration()
        val ruleset = getCivilopediaRuleset()
        UncivGame.Current.translations.translationActiveMods = ruleset.mods
        ImageGetter.setNewRuleset(ruleset)
        setSkin()
        openCivilopedia(ruleset, link = link) // Call to BaseScreen's openCivilopedia
    }

    // recreate() remains parameterless
    override fun recreate(): BaseScreen {
        stopBackgroundMapGeneration()
        return MainMenuScreen()
    }

    // resume() remains as in your file
    override fun resume() {
        // Consider adding stopBackgroundMapGeneration() here if issues arise from overlapping jobs
        // stopBackgroundMapGeneration() 
        startBackgroundMapGeneration()
    }

    override fun getShortcutDispatcherVetoer() = KeyShortcutDispatcherVeto.createTileGroupMapDispatcherVetoer()
}
