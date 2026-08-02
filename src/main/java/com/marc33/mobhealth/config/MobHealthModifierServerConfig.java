package com.marc33.mobhealth.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Reglages d'affichage imposes par le serveur.
 *
 * <p>Une configuration de type {@link ModConfig.Type#SERVER} est envoyee automatiquement a chaque
 * client au moment de la connexion : le client lit donc ces valeurs telles que le serveur les a
 * definies, sans qu'aucun paquet ne soit a ecrire. C'est ce qui permet a un serveur d'imposer
 * l'affichage des barres de vie et la visibilite des pseudos.
 *
 * <p>Le fichier vit dans le dossier du monde et non dans {@code config/} :
 * {@code <monde>/serverconfig/mobhealthmodifier-server.toml}.
 *
 * <p>Les deux interrupteurs {@code enforce*} valent {@code false} par defaut, ce qui laisse chaque
 * joueur maitre de son affichage. C'est aussi la valeur que prend la configuration lorsqu'un client
 * rejoint un serveur depourvu du mod : dans ce cas NeoForge charge les valeurs par defaut, et rien
 * n'est impose.
 */
public final class MobHealthModifierServerConfig {

    /** Structure du fichier TOML, section {@code [display]}. */
    public static final class Server {

        public final ModConfigSpec.BooleanValue enforceHealthBarSettings;
        public final ModConfigSpec.BooleanValue healthBarsEnabled;
        public final ModConfigSpec.BooleanValue showOnHostileMobs;
        public final ModConfigSpec.BooleanValue showOnPassiveMobs;
        public final ModConfigSpec.BooleanValue showOnPlayers;
        public final ModConfigSpec.DoubleValue maxRenderDistance;

        public final ModConfigSpec.BooleanValue enforceNameTagSettings;
        public final ModConfigSpec.BooleanValue hidePlayerNameTags;

        Server(ModConfigSpec.Builder builder) {
            builder.comment(
                            "Reglages d'affichage imposes aux joueurs connectes.",
                            "Tant qu'un interrupteur enforce* reste a false, chaque joueur garde",
                            "la main sur son propre fichier mobhealthmodifier-client.toml.")
                    .push("display");

            enforceHealthBarSettings = builder
                    .comment(
                            "Imposer les reglages de barres de vie ci-dessous a tous les joueurs.",
                            "Les reglages purement esthetiques (taille, decalage vertical) restent",
                            "toujours libres cote client.")
                    .define("enforceHealthBarSettings", false);

            healthBarsEnabled = builder
                    .comment("Barres de vie autorisees. Impose seulement si enforceHealthBarSettings.")
                    .define("healthBarsEnabled", true);

            showOnHostileMobs = builder
                    .comment("Barres sur les mobs hostiles. Impose seulement si enforceHealthBarSettings.")
                    .define("showOnHostileMobs", true);

            showOnPassiveMobs = builder
                    .comment("Barres sur les mobs passifs. Impose seulement si enforceHealthBarSettings.")
                    .define("showOnPassiveMobs", false);

            showOnPlayers = builder
                    .comment("Barres sur les joueurs. Impose seulement si enforceHealthBarSettings.")
                    .define("showOnPlayers", true);

            maxRenderDistance = builder
                    .comment(
                            "Distance maximale d'affichage, en blocs.",
                            "Impose seulement si enforceHealthBarSettings : voir la vie d'un joueur",
                            "de tres loin est un avantage, d'ou la presence de ce reglage ici.")
                    .defineInRange("maxRenderDistance", 24.0D, 4.0D, 64.0D);

            enforceNameTagSettings = builder
                    .comment("Imposer la visibilite des pseudos ci-dessous a tous les joueurs.")
                    .define("enforceNameTagSettings", false);

            hidePlayerNameTags = builder
                    .comment("Masquer les pseudos. Impose seulement si enforceNameTagSettings.")
                    .define("hidePlayerNameTags", true);

            builder.pop();
        }
    }

    private static final Pair<Server, ModConfigSpec> PAIR =
            new ModConfigSpec.Builder().configure(Server::new);

    /** Instance contenant les valeurs de configuration. */
    public static final Server SERVER = PAIR.getLeft();

    /** Specification remise a NeoForge pour generer, valider et synchroniser le TOML. */
    public static final ModConfigSpec SERVER_SPEC = PAIR.getRight();

    private MobHealthModifierServerConfig() {
    }

    /**
     * Enregistre la configuration serveur aupres de NeoForge.
     *
     * @param container conteneur du mod fourni par NeoForge
     */
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
    }

    /**
     * Indique si la configuration est disponible. Hors partie — dans le menu principal — elle ne
     * l'est pas, et rien ne doit alors etre impose.
     *
     * @return {@code true} si les valeurs peuvent etre lues
     */
    private static boolean isLoaded() {
        return SERVER_SPEC.isLoaded();
    }

    /**
     * @return {@code true} si le serveur impose ses reglages de barres de vie
     */
    public static boolean enforcesHealthBarSettings() {
        return isLoaded() && SERVER.enforceHealthBarSettings.get();
    }

    /**
     * @return {@code true} si le serveur impose la visibilite des pseudos
     */
    public static boolean enforcesNameTagSettings() {
        return isLoaded() && SERVER.enforceNameTagSettings.get();
    }

    /**
     * @return {@code true} si le serveur autorise les barres de vie
     */
    public static boolean areHealthBarsEnabled() {
        return SERVER.healthBarsEnabled.get();
    }

    /**
     * @return {@code true} si le serveur autorise les barres sur les mobs hostiles
     */
    public static boolean showOnHostileMobs() {
        return SERVER.showOnHostileMobs.get();
    }

    /**
     * @return {@code true} si le serveur autorise les barres sur les mobs passifs
     */
    public static boolean showOnPassiveMobs() {
        return SERVER.showOnPassiveMobs.get();
    }

    /**
     * @return {@code true} si le serveur autorise les barres sur les joueurs
     */
    public static boolean showOnPlayers() {
        return SERVER.showOnPlayers.get();
    }

    /**
     * @return la distance maximale d'affichage imposee, en blocs
     */
    public static double getMaxRenderDistance() {
        return SERVER.maxRenderDistance.get();
    }

    /**
     * @return {@code true} si le serveur exige que les pseudos soient masques
     */
    public static boolean hidePlayerNameTags() {
        return SERVER.hidePlayerNameTags.get();
    }
}
