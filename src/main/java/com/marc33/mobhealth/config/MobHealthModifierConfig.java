package com.marc33.mobhealth.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Definit et expose la configuration du mod.
 *
 * <p>Le fichier genere est {@code config/mobhealthmodifier-common.toml}. Les bornes declarees ici
 * sont appliquees par NeoForge : une valeur hors intervalle est rejetee et remplacee par le defaut.
 */
public final class MobHealthModifierConfig {

    /** Valeur la plus basse acceptee pour un multiplicateur. */
    public static final double MIN_MULTIPLIER = 0.1D;

    /** Valeur la plus haute acceptee pour un multiplicateur. */
    public static final double MAX_MULTIPLIER = 10.0D;

    /** Ciblage utilise tant que la configuration n'est pas chargee. */
    public static final MobTarget DEFAULT_TARGET = MobTarget.HOSTILE;

    /** Structure du fichier TOML, section {@code [general]}. */
    public static final class Common {

        public final ModConfigSpec.EnumValue<MobTarget> affectedMobs;
        public final ModConfigSpec.BooleanValue enableHealthModification;
        public final ModConfigSpec.DoubleValue healthMultiplier;
        public final ModConfigSpec.BooleanValue enableDamageModification;
        public final ModConfigSpec.DoubleValue damageMultiplier;

        Common(ModConfigSpec.Builder builder) {
            builder.comment("Reglages globaux appliques aux mobs qui apparaissent.")
                    .push("general");

            affectedMobs = builder
                    .comment(
                            "Quels mobs sont affectes par les multiplicateurs.",
                            "HOSTILE = zombies, squelettes, creepers, blazes, boss...",
                            "PASSIVE = vaches, moutons, villageois, golems...",
                            "ALL     = tous les mobs")
                    .defineEnum("affectedMobs", DEFAULT_TARGET);

            enableHealthModification = builder
                    .comment("Active la modification de la sante maximale des mobs.")
                    .define("enableHealthModification", true);

            healthMultiplier = builder
                    .comment(
                            "Multiplicateur applique a la sante maximale.",
                            "1.0 = normal | 2.0 = double | 0.5 = moitie")
                    .defineInRange("healthMultiplier", 1.0D, MIN_MULTIPLIER, MAX_MULTIPLIER);

            enableDamageModification = builder
                    .comment("Active la modification des degats d'attaque des mobs.")
                    .define("enableDamageModification", true);

            damageMultiplier = builder
                    .comment(
                            "Multiplicateur applique aux degats d'attaque.",
                            "1.0 = normal | 2.0 = double | 0.5 = moitie")
                    .defineInRange("damageMultiplier", 1.0D, MIN_MULTIPLIER, MAX_MULTIPLIER);

            builder.pop();
        }
    }

    private static final Pair<Common, ModConfigSpec> PAIR =
            new ModConfigSpec.Builder().configure(Common::new);

    /** Instance contenant les valeurs de configuration. */
    public static final Common COMMON = PAIR.getLeft();

    /** Specification remise a NeoForge pour generer et valider le TOML. */
    public static final ModConfigSpec COMMON_SPEC = PAIR.getRight();

    private MobHealthModifierConfig() {
    }

    /**
     * Enregistre la configuration aupres de NeoForge. A appeler depuis le constructeur du mod.
     *
     * @param container conteneur du mod fourni par NeoForge
     */
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
    }

    /**
     * Indique si le fichier de configuration est charge. Les getters renvoient leur valeur par
     * defaut tant que ce n'est pas le cas, ce qui evite une exception si un evenement arrive tot.
     *
     * @return {@code true} si les valeurs peuvent etre lues
     */
    public static boolean isLoaded() {
        return COMMON_SPEC.isLoaded();
    }

    /**
     * @return le mode de ciblage choisi, jamais {@code null}
     */
    public static MobTarget getAffectedMobs() {
        return isLoaded() ? COMMON.affectedMobs.get() : DEFAULT_TARGET;
    }

    /**
     * @return {@code true} si la sante maximale doit etre modifiee
     */
    public static boolean isHealthModificationEnabled() {
        return isLoaded() ? COMMON.enableHealthModification.get() : true;
    }

    /**
     * @return le multiplicateur de sante, entre {@value #MIN_MULTIPLIER} et {@value #MAX_MULTIPLIER}
     */
    public static double getHealthMultiplier() {
        return isLoaded() ? COMMON.healthMultiplier.get() : 1.0D;
    }

    /**
     * @return {@code true} si les degats d'attaque doivent etre modifies
     */
    public static boolean isDamageModificationEnabled() {
        return isLoaded() ? COMMON.enableDamageModification.get() : true;
    }

    /**
     * @return le multiplicateur de degats, entre {@value #MIN_MULTIPLIER} et {@value #MAX_MULTIPLIER}
     */
    public static double getDamageMultiplier() {
        return isLoaded() ? COMMON.damageMultiplier.get() : 1.0D;
    }
}
