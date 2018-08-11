package de.randombyte.byteitems

import com.google.inject.Inject
import de.randombyte.byteitems.commands.DeleteCommand
import de.randombyte.byteitems.commands.GiveCommand
import de.randombyte.byteitems.commands.ListCommand
import de.randombyte.byteitems.commands.SaveCommand
import de.randombyte.kosp.extensions.toText
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.loader.ConfigurationLoader
import org.bstats.sponge.Metrics
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.command.args.GenericArguments.*
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.game.GameReloadEvent
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.plugin.Plugin

@Plugin(id = ByteItems.ID, name = ByteItems.NAME, version = ByteItems.VERSION, authors = [(ByteItems.AUTHOR)])
class ByteItems @Inject constructor(
        @DefaultConfig(sharedRoot = false) private val configLoader: ConfigurationLoader<CommentedConfigurationNode>,
        val logger: Logger,
        val bStats: Metrics
) {
    internal companion object {

        const val ID = "byte-items"
        const val NAME = "ByteItems"
        const val VERSION = "2.2.6"
        const val AUTHOR = "RandomByte"

        const val ROOT_PERMISSION = "byteItems"

        const val ID_ARG = "id"
        const val PLAYER_ARG = "player"
        const val AMOUNT_ARG = "amount"

        const val DEFAULT_URL = "${ByteItems.ID}-database-url"
    }

    private lateinit var config: Config

    @Listener
    fun onPreInit(event: GameInitializationEvent) {
        syncConfig()
        registerCommands()
        registerService()
    }

    @Listener
    fun onReload(event: GameReloadEvent) = syncConfig()

    private fun registerService() {
        val apiImpl = ByteItemsApiImpl(getConfig = { config })
        Sponge.getServiceManager().setProvider(this, ByteItemsApi::class.java, apiImpl)
    }

    private fun registerCommands() {
        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .child(CommandSpec.builder()
                        .permission("$ROOT_PERMISSION.save")
                        .arguments(string(ID_ARG.toText()))
                        .executor(SaveCommand(config))
                        .build(), "save")
                .child(CommandSpec.builder()
                        .permission("$ROOT_PERMISSION.give")
                        .arguments(playerOrSource(PLAYER_ARG.toText()), string(ID_ARG.toText()), optional(integer(AMOUNT_ARG.toText())))
                        .executor(GiveCommand(config))
                        .build(), "give")
                .child(CommandSpec.builder()
                        .permission("$ROOT_PERMISSION.list")
                        .executor(ListCommand(config))
                        .build(), "list")
                .child(CommandSpec.builder()
                        .permission("$ROOT_PERMISSION.delete")
                        .arguments(string(ID_ARG.toText()))
                        .executor(DeleteCommand(config))
                        .build(), "delete")
                .build(), "byteitems", "bi")
    }

    private fun syncConfig() {
        configLoader.load().let {
            it.getNode(DEFAULT_URL).getString("jdbc:h2:${ByteItems.ID}").let { config = Config(it) }
            config.let { (url, _) -> it.getNode(DEFAULT_URL).value = url }
            configLoader.save(it)
        }
        logger.info("$NAME loaded: $VERSION")
    }
}
