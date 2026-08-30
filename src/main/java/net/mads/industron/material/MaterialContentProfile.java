package net.mads.industron.material;

/**
 * Controls which game-content forms a compound definition is allowed to register.
 *
 * <p>This is deliberately separate from chemistry classification. Chemistry still derives phase,
 * topology, tier and physical properties from {@code .contains(...)}. The profile only answers
 * what content the definition owns.</p>
 */
public enum MaterialContentProfile {
    /** Normal compound/alloy/polymer behaviour derived from chemistry. */
    AUTO,

    /** A reviewed mineral phase that exists only as a normal dust item. */
    MINERAL_DUST,

    /** A reviewed ore mineral. Ore hosts, worldgen and ore-processing forms are generated automatically. */
    ORE
}
