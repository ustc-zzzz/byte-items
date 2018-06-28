package de.randombyte.byteitems.commands

import de.randombyte.byteitems.ByteItems
import de.randombyte.byteitems.Config
import de.randombyte.kosp.extensions.give
import de.randombyte.kosp.extensions.green
import de.randombyte.kosp.extensions.orNull
import de.randombyte.kosp.extensions.toText
import org.spongepowered.api.command.CommandException
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.spec.CommandExecutor
import org.spongepowered.api.entity.living.player.Player

class GiveCommand(private val config: Config) : CommandExecutor {
    override fun execute(src: CommandSource, args: CommandContext): CommandResult {
        val targetPlayer = args.getOne<Player>(ByteItems.PLAYER_ARG).get()
        val amount = args.getOne<Int>(ByteItems.AMOUNT_ARG).orNull()
        val id = args.getOne<String>(ByteItems.ID_ARG).get()

        val snapshot = config.load(id) ?: throw CommandException("Item '$id' is not available!".toText())
        val itemStack = snapshot.createStack().apply { amount?.let { quantity = it.coerceAtLeast(1) } }

        targetPlayer.apply { give(itemStack) }.sendMessage("Given '$id' to ${targetPlayer.name}!".green())
        return CommandResult.success()
    }
}