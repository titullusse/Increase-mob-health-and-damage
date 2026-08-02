package com.marc33.mobhealth.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Configuration purement visuelle, propre a chaque joueur.
 *
 * <p>Le fichier genere est {@code config/mobhealthmodifier-client.toml}. Contrairement a la config
 * commune, il n'est jamais impose par le serveur : chacun regle l'affichage comme il l'entend.
 */
public final class MobHealthModifierClientConfig {

    /** Structure du fichier TOML, section {@code [display]}. */
    public static final class Client {

        public final ModConfigSpec.BooleanValue enableHealthBars;
        public final ModConfigSpec.BooleanValue showOnHostileMobs;
        public final ModConfigSpec.BooleanValue showOnPassiveMobs;
        public final ModConfigSpec.BooleanValue showOnPlayers;
        public final ModConfigSpec.BooleanValue hidePlayerNameTags;
        public final ModConfigSpec.DoubleValue maxRenderDistance;
        public final ModConfigSpec.IntValue barWidth;
        public final ModConfigSpec.IntValue barHeight;
        public final ModConfigSpec.DoubleValue verticalOffset;

        Client(ModConfigSpec.Builder builder) {
            builder.comment("Affichage des barres de vie. Reglages locaux, sans effet sur le jeu.")
                    .push("display");

            enableHealthBars = builder
                    .comment("Interrupteur general des barres de vie.")
                    .define("enableHealthBars", true);

            showOnHostileMobs = builder
                    .comment("Afficher une barre au-dessus des mobs hostiles.")
                    .define("showOnHostileMobs", true);

            showOnPassiveMobs = builder
                    .comment("Afficher une barre au-dessus des mobs passifs.")
                    .define("showOnPassiveMobs", false);

            showOnPlayers = builder
                    .comment(
                            "Afficher une barre au-dessus des joueurs.",
                            "Vous compris, si vous passez en vue a la troisieme personne.")
                    .define("showOnPlayers", true);

            hidePlayerNameTags = builder
                    .comment(
                            "Masquer le pseudo affiche au-dessus des joueurs.",
                            "Pratique pour ne garder que la barre de vie.",
                            "N'affecte ni les mobs nommes, ni la liste des joueurs (touche Tab).")
                    .define("hidePlayerNameTags", false);

            maxRenderDistance = builder
                    .comment("Distance maximale d'affichage d'une barre, en blocs.")
                    .defineInRange("maxRenderDistance", 24.0D, 4.0D, 64.0D);

            barWidth = builder
                    .comment("Largeur de la barre, en pixels (40 = un bloc de large).")
                    .defineInRange("barWidth", 40, 8, 120);

            barHeight = builder
                    .comment("Hauteur de la barre, en pixels.")
                    .defineInRange("barHeight", 5, 1, 20);

            verticalOffset = builder
                    .comment("Decalage vertical de la barre, en blocs. Negatif = plus bas.")
                    .defineInRange("verticalOffset", 0.3D, -2.0D, 2.0D);

            builder.pop();
        }
    }

    private static final Pair<Client, ModConfigSpec> PAIR =
            new ModConfigSpec.Builder().configure(Client::new);

    /** Instance contenant les valeurs de configuration. */
    public static final Client CLIENT = PAIR.getLeft();

    /** Specification remise a NeoForge pour generer et valider le TOML. */
    public static final ModConfigSpec CLIENT_SPEC = PAIR.getRight();

    private MobHealthModifierClientConfig() {
    }

    /**
     * Enregistre la configuration client aupres de NeoForge.
     *
     * @param container conteneur du mod fourni par NeoForge
     */
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }

    private static boolean isLoaded() {
        return CLIENT_SPEC.isLoaded();
    }

    /**
     * @return {@code true} si les barres de vie doivent etre dessinees
     */
    public static boolean areHealthBarsEnabled() {
        return isLoaded() && CLIENT.enableHealthBars.get();
    }

    /**
     * @return {@code true} si les mobs hostiles portent une barre
     */
    public static boolean showOnHostileMobs() {
        return isLoaded() && CLIENT.showOnHostileMobs.get();
    }

    /**
     * @return {@code true} si les mobs passifs portent une barre
     */
    public static boolean showOnPassiveMobs() {
        return isLoaded() && CLIENT.showOnPassiveMobs.get();
    }

    /**
     * @return {@code true} si les joueurs portent une barre
     */
    public static boolean showOnPlayers() {
        return isLoaded() && CLIENT.showOnPlayers.get();
    }

    /**
     * @return {@code true} si les pseudos des joueurs doivent etre masques
     */
    public static boolean hidePlayerNameTags() {
        return isLoaded() && CLIENT.hidePlayerNameTags.get();
    }

    /**
     * @return la distance maximale d'affichage, en blocs
     */
    public static double getMaxRenderDistance() {
        return isLoaded() ? CLIENT.maxRenderDistance.get() : 24.0D;
    }

    /**
     * @return la largeur de la barre, en pixels
     */
    public static int getBarWidth() {
        return isLoaded() ? CLIENT.barWidth.get() : 40;
    }

    /**
     * @return la hauteur de la barre, en pixels
     */
    public static int getBarHeight() {
        return isLoaded() ? CLIENT.barHeight.get() : 5;
    }

    /**
     * @return le decalage vertical de la barre, en blocs
     */
    public static double getVerticalOffset() {
        return isLoaded() ? CLIENT.verticalOffset.get() : 0.3D;
    }
}
