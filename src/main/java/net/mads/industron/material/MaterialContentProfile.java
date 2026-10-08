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

    /** Biology owns its explicit process feed forms; never infer metal, molten or tool forms. */
    BIOLOGICAL,

    /** A reviewed mineral phase that exists only as a normal dust item. */
    MINERAL_DUST,

    /** A reviewed ore mineral. Ore hosts, worldgen and ore-processing forms are generated automatically. */
    ORE,

    /**
     * A formable clay body. Dust, wet clay, unfired brick and fired brick content plus the
     * complete processing chain are generated from composition; definitions do not list forms.
     */
    CLAY,

    /**
     * A fired ceramic brick family that starts from an already-existing raw material instead of
     * wet clay. It owns only the individual fired and cracked brick forms; block stacking stays
     * recipe-specific so this profile does not create a generic cracked-bricks MaterialPart.
     */
    CERAMIC_BRICK
}
