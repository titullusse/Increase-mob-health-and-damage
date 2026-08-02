package com.marc33.mobhealth.client;

import com.marc33.mobhealth.config.MobHealthModifierServerConfig;

/**
 * Arbitre entre les preferences locales du joueur et les reglages imposes par le serveur.
 *
 * <p>C'est la seule source de verite consultee par le rendu : chaque question a une reponse unique,
 * qu'elle vienne du serveur ou du fichier client. La regle est constante — si le serveur impose le
 * groupe de reglages concerne, sa valeur gagne ; sinon celle du joueur s'applique.
 *
 * <p>Deux garde-fous rendent ce basculement sur : hors partie la configuration serveur n'est pas
 * chargee et n'impose donc rien, et lorsqu'un client rejoint un serveur depourvu du mod NeoForge
 * charge les valeurs par defaut, dans lesquelles les interrupteurs {@code enforce*} sont a
 * {@code false}.
 */
public final class DisplayPolicy {

    private DisplayPolicy() {
    }

    /**
     * @return {@code true} si les barres de vie doivent etre dessinees
     */
    public static boolean areHealthBarsEnabled() {
        return MobHealthModifierServerConfig.enforcesHealthBarSettings()
                ? MobHealthModifierServerConfig.areHealthBarsEnabled()
                : MobHealthModifierClientConfig.areHealthBarsEnabled();
    }

    /**
     * @return {@code true} si les mobs hostiles portent une barre
     */
    public static boolean showOnHostileMobs() {
        return MobHealthModifierServerConfig.enforcesHealthBarSettings()
                ? MobHealthModifierServerConfig.showOnHostileMobs()
                : MobHealthModifierClientConfig.showOnHostileMobs();
    }

    /**
     * @return {@code true} si les mobs passifs portent une barre
     */
    public static boolean showOnPassiveMobs() {
        return MobHealthModifierServerConfig.enforcesHealthBarSettings()
                ? MobHealthModifierServerConfig.showOnPassiveMobs()
                : MobHealthModifierClientConfig.showOnPassiveMobs();
    }

    /**
     * @return {@code true} si les joueurs portent une barre
     */
    public static boolean showOnPlayers() {
        return MobHealthModifierServerConfig.enforcesHealthBarSettings()
                ? MobHealthModifierServerConfig.showOnPlayers()
                : MobHealthModifierClientConfig.showOnPlayers();
    }

    /**
     * @return la distance maximale d'affichage, en blocs
     */
    public static double getMaxRenderDistance() {
        return MobHealthModifierServerConfig.enforcesHealthBarSettings()
                ? MobHealthModifierServerConfig.getMaxRenderDistance()
                : MobHealthModifierClientConfig.getMaxRenderDistance();
    }

    /**
     * @return {@code true} si les pseudos des joueurs doivent etre masques
     */
    public static boolean hidePlayerNameTags() {
        return MobHealthModifierServerConfig.enforcesNameTagSettings()
                ? MobHealthModifierServerConfig.hidePlayerNameTags()
                : MobHealthModifierClientConfig.hidePlayerNameTags();
    }

    /**
     * Taille et position de la barre restent toujours locales : elles ne procurent aucun avantage,
     * et un serveur n'a pas de raison de dicter l'esthetique de chacun.
     *
     * @return la largeur de la barre, en pixels
     */
    public static int getBarWidth() {
        return MobHealthModifierClientConfig.getBarWidth();
    }

    /**
     * @return la hauteur de la barre, en pixels
     */
    public static int getBarHeight() {
        return MobHealthModifierClientConfig.getBarHeight();
    }

    /**
     * @return le decalage vertical de la barre, en blocs
     */
    public static double getVerticalOffset() {
        return MobHealthModifierClientConfig.getVerticalOffset();
    }
}
