package com.marc33.mobhealth.events;

import com.marc33.mobhealth.MobHealthModifier;
import com.marc33.mobhealth.config.MobHealthModifierConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
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

        if (MobHealthModifierConfig.isHealthModificationEnabled()) {
            applyHealth(mob, MobHealthModifierConfig.getHealthMultiplier());
        }

        if (MobHealthModifierConfig.isDamageModificationEnabled()) {
            applyDamage(mob, MobHealthModifierConfig.getDamageMultiplier());
        }
    }

    /**
     * Applique le multiplicateur de sante et remet le mob a sa nouvelle sante maximale.
     *
     * @param mob mob a modifier
     * @param multiplier facteur applique a la valeur de base de {@code MAX_HEALTH}
     */
    private static void applyHealth(Mob mob, double multiplier) {
        if (multiplier == 1.0D) {
            return;
        }

        AttributeInstance healthAttribute = mob.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttribute == null) {
            return;
        }

        double previous = healthAttribute.getBaseValue();
        healthAttribute.setBaseValue(previous * multiplier);

        // setBaseValue ne soigne pas le mob : sans cela il apparaitrait blesse.
        mob.setHealth(mob.getMaxHealth());

        MobHealthModifier.LOGGER.debug(
                "Sante de {} : {} -> {}", mob.getType(), previous, healthAttribute.getBaseValue());
    }

    /**
     * Applique le multiplicateur de degats d'attaque.
     *
     * @param mob mob a modifier
     * @param multiplier facteur applique a la valeur de base de {@code ATTACK_DAMAGE}
     */
    private static void applyDamage(Mob mob, double multiplier) {
        if (multiplier == 1.0D) {
            return;
        }

        AttributeInstance damageAttribute = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damageAttribute == null) {
            return;
        }

        double previous = damageAttribute.getBaseValue();
        damageAttribute.setBaseValue(previous * multiplier);

        MobHealthModifier.LOGGER.debug(
                "Degats de {} : {} -> {}", mob.getType(), previous, damageAttribute.getBaseValue());
    }
}
