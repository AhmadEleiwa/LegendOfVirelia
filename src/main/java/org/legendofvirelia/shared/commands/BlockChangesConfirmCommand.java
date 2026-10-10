package org.legendofvirelia.shared.commands;

import java.util.List;
import org.legendofvirelia.shared.ClientWorldState;
import org.legendofvirelia.shared.command.ClientCommand;

public final class BlockChangesConfirmCommand implements ClientCommand {
    public final List<BlockChange> changes;

    public BlockChangesConfirmCommand(List<BlockChange> changes) {
        this.changes = List.copyOf(changes);
    }

    @Override
    public void execute(ClientWorldState world) {
        world.applyBlockChanges(changes);
    }
}
