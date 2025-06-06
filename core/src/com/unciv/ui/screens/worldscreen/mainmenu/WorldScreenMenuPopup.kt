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

import com.helpshift.Helpshift
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

                // 2. Create a specific map for your Custom Issue Fields (CIFs).
                val customIssueFields = HashMap<String, String>()

                // 3. Populate the CIF map with your game data.
                val userId = worldScreen.viewingCiv.civName
                customIssueFields["user_id"] = userId

                worldScreen.game.gameInfo?.let { gameInfo ->
                    customIssueFields["game_id"] = gameInfo.gameId
                    customIssueFields["mods"] = gameInfo.gameParameters.mods.joinToString(", ")
                }

                // 4. Add your CIFs map to the main config map.
                // For SDK X, the key is "customIssueFields".
                config["customIssueFields"] = customIssueFields

                // 5. Call showFAQs with the activity AND the configuration map.
//                 Helpshift.showFAQs(worldScreen.game.platformSpecific, config)

            } catch (e: Exception) {
                // If anything fails, show the FAQs without custom data.
//                 Helpshift.showFAQs(worldScreen.game.platformSpecific)
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
