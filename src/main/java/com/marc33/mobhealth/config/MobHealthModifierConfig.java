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

    /**
     * Plafond des multiplicateurs, si haut qu'il ne peut jamais etre atteint utilement.
     *
     * <p>La vraie limite ne vient pas d'ici mais du jeu lui-meme : chaque attribut vanilla declare
     * son intervalle, {@code MAX_HEALTH} s'arretant a 1024 points de vie et {@code ATTACK_DAMAGE} a
     * 2048 degats. Au-dela, Minecraft ramene la valeur a cette borne. Le multiplicateur le plus
     * eleve ayant encore un effet est donc de l'ordre de quelques milliers, dans le cas extreme d'un
     * mob partant d'une valeur inferieure a 1 ; pour un zombie et ses 20 points de vie, tout ce qui
     * depasse 51 revient au meme.
     *
     * <p>{@link Double#MAX_VALUE} conviendrait tout autant, mais NeoForge ne sait ecrire une borne
     * ouverte que pour les entiers : le fichier afficherait alors {@code Range: 0.1 ~
     * 1.7976931348623157E308}. Une valeur ronde reste lisible sans rien restreindre.
     */
    public static final double MAX_MULTIPLIER = 1_000_000.0D;

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
                            "1.0 = normal | 2.0 = double | 0.5 = moitie",
                            "Aucun plafond ici, mais Minecraft limite la sante a 1024 points :",
                            "au-dela le mob restera a 1024 (soit x51 environ pour un zombie).")
                    .defineInRange("healthMultiplier", 1.0D, MIN_MULTIPLIER, MAX_MULTIPLIER);

            enableDamageModification = builder
                    .comment("Active la modification des degats d'attaque des mobs.")
                    .define("enableDamageModification", true);

            damageMultiplier = builder
                    .comment(
                            "Multiplicateur applique aux degats d'attaque.",
                            "1.0 = normal | 2.0 = double | 0.5 = moitie",
                            "Aucun plafond ici, mais Minecraft limite les degats a 2048.")
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
     * @return le multiplicateur de sante, au minimum {@value #MIN_MULTIPLIER}
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
     * @return le multiplicateur de degats, au minimum {@value #MIN_MULTIPLIER}
     */
    public static double getDamageMultiplier() {
        return isLoaded() ? COMMON.damageMultiplier.get() : 1.0D;
    }
}
