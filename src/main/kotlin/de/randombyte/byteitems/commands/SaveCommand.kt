package de.randombyte.byteitems.commands

import de.randombyte.byteitems.ByteItems
import de.randombyte.byteitems.Config
import de.randombyte.kosp.PlayerExecutedCommand
import de.randombyte.kosp.extensions.green
import de.randombyte.kosp.extensions.orNull
import de.randombyte.kosp.extensions.toText
import org.spongepowered.api.command.CommandException
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.data.type.HandTypes
import org.spongepowered.api.entity.living.player.Player

internal class SaveCommand(private val config: Config) : PlayerExecutedCommand() {
    override fun executedByPlayer(player: Player, args: CommandContext): CommandResult {
        val id = args.getOne<String>(ByteItems.ID_ARG).get()
        val item = player.getItemInHand(HandTypes.MAIN_HAND).orNull()
        val snapshot = item?.createSnapshot() ?: throw CommandException("Hold item in main hand while saving!".toText())

        if (!config.save(id, snapshot)) throw CommandException("ID '$id' is already in use!".toText())
        player.sendMessage("Saved ItemStack '$id'!".green())
        return CommandResult.success()
    }
}