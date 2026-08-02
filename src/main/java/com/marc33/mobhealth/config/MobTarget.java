package com.marc33.mobhealth.config;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Determine quels mobs recoivent les multiplicateurs.
 *
 * <p>Choisi dans le TOML via l'option {@code affectedMobs} de la section {@code [general]}.
 */
public enum MobTarget {

    /** Tous les mobs, hostiles comme passifs. */
    ALL {
        @Override
        public boolean matches(Mob mob) {
            return true;
        }
    },

    /** Uniquement les mobs hostiles : zombies, squelettes, creepers, blazes, boss, etc. */
    HOSTILE {
        @Override
        public boolean matches(Mob mob) {
            return isHostile(mob);
        }
    },

    /** Uniquement les mobs non hostiles : vaches, moutons, villageois, golems, etc. */
    PASSIVE {
        @Override
        public boolean matches(Mob mob) {
            return !isHostile(mob);
        }
    };

    /**
     * Indique si ce mode de ciblage concerne le mob donne.
     *
     * @param mob mob a tester
     * @return {@code true} si les multiplicateurs doivent lui etre appliques
     */
    public abstract boolean matches(Mob mob);

    /**
     * Teste l'hostilite d'un mob selon deux criteres complementaires.
     *
     * <p>L'interface {@link Enemy} est le marqueur vanilla de l'hostilite. Elle est portee par
     * {@code Monster} — donc par les zombies, squelettes, creepers, araignees, endermen, piglins,
     * le wither… — mais aussi, en dehors de cette hierarchie, par les slimes, ghasts, phantoms,
     * shulkers, hoglins, zoglins et l'ender dragon.
     *
     * <p>La categorie de spawn {@link MobCategory#MONSTER} sert de filet de securite : un mob ajoute
     * par un autre mod peut se declarer hostile par sa categorie sans implementer {@link Enemy}.
     *
     * @param mob mob a tester
     * @return {@code true} si le mob est considere comme hostile
     */
    private static boolean isHostile(Mob mob) {
        return mob instanceof Enemy || mob.getType().getCategory() == MobCategory.MONSTER;
    }
}
