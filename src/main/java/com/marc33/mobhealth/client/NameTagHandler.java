package com.marc33.mobhealth.client;

import com.marc33.mobhealth.MobHealthModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;

/**
 * Masque le pseudo affiche au-dessus des joueurs lorsque la configuration client le demande.
 *
 * <p>Seul l'affichage dans le monde est concerne : la liste des joueurs (touche Tab), le chat et
 * les plaques de nom des mobs nommes restent intacts.
 */
@EventBusSubscriber(modid = MobHealthModifier.MOD_ID, value = Dist.CLIENT)
public final class NameTagHandler {

    private NameTagHandler() {
    }

    /**
     * Annule le rendu de la plaque de nom des joueurs si l'option est active.
     *
     * @param event evenement declenche juste avant le rendu d'une plaque de nom
     */
    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!MobHealthModifierClientConfig.hidePlayerNameTags()) {
            return;
        }

        if (event.getEntity() instanceof Player) {
            event.setCanRender(TriState.FALSE);
        }
    }
}
