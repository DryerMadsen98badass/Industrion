package net.mads.industron.block.loot;

/**
 * Explicit block loot policy for assembled technical blocks.
 *
 * <p>Implementing this marker means normal block breaking must use the matching
 * Assembly recipe as its salvage source instead of dropping the assembled block
 * itself. Merely having an Assembly recipe does not opt a block into salvage.</p>
 */
public interface AssemblySalvageBlock {
}
