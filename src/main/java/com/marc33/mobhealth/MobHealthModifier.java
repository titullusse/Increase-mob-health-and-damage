package com.marc33.mobhealth;

import com.marc33.mobhealth.client.MobHealthModifierClientConfig;
import com.marc33.mobhealth.config.MobHealthModifierConfig;
import com.marc33.mobhealth.config.MobHealthModifierServerConfig;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Point d'entree du mod.
 *
 * <p>Le mod ne fait qu'une seule chose : enregistrer sa configuration TOML au demarrage. Toute la
 * logique de jeu vit dans {@link com.marc33.mobhealth.events.MobAttributeHandler}, qui est branche
 * sur le bus d'evenements via {@code @EventBusSubscriber}.
 */
@Mod(MobHealthModifier.MOD_ID)
public class MobHealthModifier {

    /** Identifiant unique du mod, partage avec {@code neoforge.mods.toml}. */
    public static final String MOD_ID = "mobhealthmodifier";

    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Constructeur appele par NeoForge lors du chargement du mod.
     *
     * @param modEventBus bus d'evenements propre au mod (chargement, enregistrements)
     * @param modContainer conteneur de ce mod, utilise pour enregistrer la configuration
     */
    public MobHealthModifier(IEventBus modEventBus, ModContainer modContainer) {
        MobHealthModifierConfig.register(modContainer);

        // Une config de type SERVER est synchronisee vers chaque client a la connexion : c'est ce
        // qui permet au serveur d'imposer l'affichage des barres et des pseudos.
        MobHealthModifierServerConfig.register(modContainer);

        // Une config de type CLIENT n'est chargee que sur un client ; l'enregistrer depuis le
        // constructeur commun est sans effet sur un serveur dedie.
        MobHealthModifierClientConfig.register(modContainer);

        LOGGER.info("Mob Health Modifier charge");
    }
}
