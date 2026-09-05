package com.marc33.mobhealth.events;

import com.marc33.mobhealth.MobHealthModifier;
import com.marc33.mobhealth.config.MobHealthModifierConfig;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Applique les multiplicateurs de la configuration aux mobs qui apparaissent dans le monde.
 *
 * <p>L'evenement {@link EntityJoinLevelEvent} se declenche aussi bien au spawn qu'au rechargement
 * d'un chunk deja visite. Sans garde-fou, un mob verrait donc ses attributs multiplies a nouveau a
 * chaque rechargement. Un marqueur est ecrit dans les donnees persistantes de l'entite pour que la
 * modification n'ait lieu qu'une seule fois par mob.
 */
@EventBusSubscriber(modid = MobHealthModifier.MOD_ID)
public final class MobAttributeHandler {

    /** Cle NBT marquant un mob deja traite, conservee dans les donnees persistantes de l'entite. */
    private static final String MODIFIED_TAG = MobHealthModifier.MOD_ID + ":modified";

    private MobAttributeHandler() {
    }

    /**
     * Intercepte l'arrivee d'une entite dans le monde et modifie ses attributs si c'est un mob.
     *
     * @param event evenement declenche par NeoForge a chaque entite ajoutee au monde
     */
    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        // Les attributs sont calcules cote serveur puis synchronises ; inutile de toucher au client.
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        applyModifications(mob);
    }

    /**
     * Multiplie la sante maximale et les degats d'attaque du mob selon la configuration.
     *
     * <p>Le mob doit d'abord correspondre au ciblage {@code affectedMobs} (hostiles, passifs ou
     * tous). Un mob qui ne possede pas l'attribut vise (par exemple un creeper, qui n'a pas de
     * degats d'attaque : il explose) est simplement ignore pour cet attribut.
     *
     * @param mob mob venant d'entrer dans le monde
     */
    private static void applyModifications(Mob mob) {
        // Filtre avant le marqueur : un mob hors ciblage repart intact, sans marqueur, et sera donc
        // reexamine s'il revient plus tard avec une configuration differente.
        if (!MobHealthModifierConfig.getAffectedMobs().matches(mob)) {
            return;
        }

        CompoundTag persistentData = mob.getPersistentData();
        if (persistentData.getBoolean(MODIFIED_TAG)) {
            return;
        }
        persistentData.putBoolean(MODIFIED_TAG, true);

        if (MobHealthModifierConfig.isHealthModificationEnabled()
                && scale(mob, Attributes.MAX_HEALTH, MobHealthModifierConfig.getHealthMultiplier())) {
            // setBaseValue ne soigne pas le mob : sans cela il apparaitrait blesse.
            mob.setHealth(mob.getMaxHealth());
        }

        if (MobHealthModifierConfig.isDamageModificationEnabled()) {
            scale(mob, Attributes.ATTACK_DAMAGE, MobHealthModifierConfig.getDamageMultiplier());
        }
    }

    /**
     * Multiplie la valeur de base d'un attribut, en respectant les bornes du jeu.
     *
     * <p>Chaque attribut vanilla declare son propre intervalle : {@code MAX_HEALTH} s'arrete a 1024
     * et {@code ATTACK_DAMAGE} a 2048. Minecraft ramene de lui-meme la valeur effective dans cet
     * intervalle, mais pas la valeur de base stockee. On passe donc explicitement par
     * {@code sanitizeValue} afin que la valeur ecrite corresponde a celle reellement utilisee,
     * plutot que de laisser un nombre sans rapport dans les donnees de l'entite.
     *
     * @param mob mob a modifier
     * @param attribute attribut vise
     * @param multiplier facteur applique a la valeur de base
     * @return {@code true} si l'attribut a effectivement ete modifie
     */
    private static boolean scale(Mob mob, Holder<Attribute> attribute, double multiplier) {
        if (multiplier == 1.0D) {
            return false;
        }

        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null) {
            return false;
        }

        double previous = instance.getBaseValue();
        double requested = previous * multiplier;
        double applied = attribute.value().sanitizeValue(requested);
        instance.setBaseValue(applied);

        if (applied < requested) {
            MobHealthModifier.LOGGER.debug(
                    "{} : {} demande {} mais le jeu plafonne a {}",
                    mob.getType(), attribute.value().getDescriptionId(), requested, applied);
        } else {
            MobHealthModifier.LOGGER.debug(
                    "{} : {} passe de {} a {}",
                    mob.getType(), attribute.value().getDescriptionId(), previous, applied);
        }

        return true;
    }
}
