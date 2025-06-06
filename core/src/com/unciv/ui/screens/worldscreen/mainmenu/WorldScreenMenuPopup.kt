package com.unciv.ui.screens.worldscreen.mainmenu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.unciv.ui.components.input.KeyboardBinding
import com.unciv.ui.components.input.onLongPress
import com.unciv.ui.popups.Popup
import com.unciv.ui.screens.basescreen.BaseScreen
import com.unciv.ui.screens.savescreens.LoadGameScreen
import com.unciv.ui.screens.victoryscreen.VictoryScreen
import com.unciv.ui.screens.worldscreen.WorldScreen

import com.unciv.UncivGame
import java.util.HashMap

/** The in-game menu called from the "Hamburger" button top-left
 *
 *  Popup automatically opens as soon as it's initialized
 */
class WorldScreenMenuPopup(
    val worldScreen: WorldScreen,
    expertMode: Boolean = false
) : Popup(worldScreen, scrollable = Scrollability.All) {
    private val singleColumn: Boolean
    private fun <T: Actor?> Cell<T>.nextColumn() {
        if (!singleColumn && column == 0) return
        row()
    }

    init {
        worldScreen.autoPlay.stopAutoPlay()
        defaults().fillX()

        val showSave = !worldScreen.gameInfo.gameParameters.isOnlineMultiplayer
        val showMusic = worldScreen.game.musicController.isMusicAvailable()
        val showConsole = showSave && expertMode
        val buttonCount = 8 + (if (showSave) 1 else 0) + (if (showMusic) 1 else 0) + (if (showConsole) 1 else 0)

        val emptyPrefHeight = this.prefHeight
        val firstCell = addButton("Main menu") {
            worldScreen.game.goToMainMenu()
        }
        singleColumn = worldScreen.isCrampedPortrait() ||
            2 * prefWidth > maxPopupWidth ||  // Very coarse: Assume width of translated "Main menu" is representative
            buttonCount * (prefHeight - emptyPrefHeight) + emptyPrefHeight < maxPopupHeight
        firstCell.nextColumn()

        addButton("Civilopedia", KeyboardBinding.Civilopedia) {
            close()
            worldScreen.openCivilopedia()
        }.nextColumn()
        if (showSave)
            addButton("Save game", KeyboardBinding.SaveGame) {
                close()
                worldScreen.openSaveGameScreen()
            }.nextColumn()
        addButton("Load game", KeyboardBinding.LoadGame) {
            close()
            worldScreen.game.pushScreen(LoadGameScreen())
        }.nextColumn()
        addButton("Start new game", KeyboardBinding.NewGame) {
            close()
            worldScreen.openNewGameScreen()
        }.nextColumn()
        addButton("Victory status", KeyboardBinding.VictoryScreen) {
            close()
            worldScreen.game.pushScreen(VictoryScreen(worldScreen))
        }.nextColumn()
        val optionsCell = addButton("Options", KeyboardBinding.Options) {
            close()
            worldScreen.openOptionsPopup()
        }
        optionsCell.actor.onLongPress {
            close()
            worldScreen.openOptionsPopup(withDebug = true)
        }
        optionsCell.nextColumn()

        val faqCell = addButton("FAQ", null) {
            try {
                // 1. Create the main configuration map for the Helpshift API call.
                val config = HashMap<String, Any>()

                // 2. Create the top-level map for CIFs using the "cifs" key.
                val cifsMap = HashMap<String, Any>() // This will hold all your CIFs

                // 3. Populate the CIFs with their type and value.
                val userCiv = worldScreen.viewingCiv.civName

                // User ID CIF
                val userCivField = HashMap<String, String>()
                userCivField["type"] = "singleline" // As configured in Helpshift
                userCivField["value"] = userCiv
                cifsMap["user_civ"] = userCivField // Use the CIF key you configured in Helpshift

                worldScreen.game.gameInfo?.let { gameInfo ->
                    // Game ID CIF
                    val gameIdField = HashMap<String, String>()
                    gameIdField["type"] = "singleline" // As configured in Helpshift
                    gameIdField["value"] = gameInfo.gameId
                    cifsMap["game_id"] = gameIdField // Use the CIF key

                    // Mods CIF
                    val modsField = HashMap<String, String>()
                    modsField["type"] = "multiline" // As configured in Helpshift
                    modsField["value"] = gameInfo.gameParameters.mods.joinToString(", ")
                    cifsMap["mods"] = modsField // Use the CIF key
                }

                // 4. Add the CIFs map to the main configuration map using the correct key "cifs".
                config["cifs"] = cifsMap

                // 5. Use your existing interface to show the FAQs.
                // This config, including the CIFs, will be used when a new conversation
                // is started from the FAQ screen.
                UncivGame.Current.platformBridge.showHelpshiftFAQs(config)

            } catch (e: Exception) {
                // Fallback call using the default empty map for options
                UncivGame.Current.platformBridge.showHelpshiftFAQs()
            }
            close()
        }
        faqCell.row()
        
        if (showMusic)
            addButton("Music", KeyboardBinding.MusicPlayer) {
                close()
                WorldScreenMusicPopup(worldScreen).open(force = true)
            }.nextColumn()

        if (showConsole)
            addButton("Developer Console", KeyboardBinding.DeveloperConsole) {
                close()
                worldScreen.openDeveloperConsole()
            }.nextColumn()
        
        addButton("Exit") {
            close()
            Gdx.app.exit()
        }.apply { actor.style = BaseScreen.skin.get("negative", TextButtonStyle::class.java) }
            .nextColumn()

        addCloseButton().run { colspan(if (singleColumn || column == 1) 1 else 2) }
        pack()

        open(force = true)
    }
}
