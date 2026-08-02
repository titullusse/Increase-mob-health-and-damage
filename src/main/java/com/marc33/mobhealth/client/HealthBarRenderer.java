package com.marc33.mobhealth.client;

import com.marc33.mobhealth.MobHealthModifier;
import com.marc33.mobhealth.config.MobTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix4f;

/**
 * Dessine une barre de vie facon barre de progression au-dessus des entites vivantes.
 *
 * <p>Purement visuel et purement local : rien n'est envoye au serveur, et la sante affichee est
 * celle deja connue du client.
 */
@EventBusSubscriber(modid = MobHealthModifier.MOD_ID, value = Dist.CLIENT)
public final class HealthBarRenderer {

    /** Cadre et separateur, noir opaque. */
    private static final int BORDER_COLOR = 0xFF000000;

    /** Portion vide de la barre, gris tres sombre translucide. */
    private static final int EMPTY_COLOR = 0xB0202020;

    /** Epaisseur du cadre, dans le repere de la barre. */
    private static final float BORDER_THICKNESS = 1.0F;

    /**
     * Echelle du repere local. Identique a celle des plaques de nom vanilla : une unite vaut
     * 0.025 bloc, et l'axe Y est inverse (les valeurs negatives montent).
     */
    private static final float SCALE = 0.025F;

    private HealthBarRenderer() {
    }

    /**
     * Dessine la barre une fois le modele de l'entite rendu.
     *
     * <p>A ce stade la pile de matrices est revenue a l'origine de l'entite, exactement comme
     * lorsque le jeu s'apprete a dessiner la plaque de nom.
     *
     * @param event evenement de fin de rendu d'une entite vivante
     */
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!shouldRender(entity)) {
            return;
        }

        renderBar(
                entity,
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPartialTick());
    }

    /**
     * Decide si une entite merite une barre de vie.
     *
     * @param entity entite en cours de rendu
     * @return {@code true} si la barre doit etre dessinee
     */
    private static boolean shouldRender(LivingEntity entity) {
        if (!MobHealthModifierClientConfig.areHealthBarsEnabled()) {
            return false;
        }

        if (!entity.isAlive() || entity.isInvisible() || entity.isSpectator()) {
            return false;
        }

        if (entity.getMaxHealth() <= 0.0F) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();

        // En vue subjective, une barre collee a la camera n'aurait aucun sens.
        if (entity == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
            return false;
        }

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        double maxDistance = MobHealthModifierClientConfig.getMaxRenderDistance();
        if (dispatcher.distanceToSqr(entity) > maxDistance * maxDistance) {
            return false;
        }

        if (entity instanceof Player) {
            return MobHealthModifierClientConfig.showOnPlayers();
        }

        if (entity instanceof Mob mob) {
            return MobTarget.HOSTILE.matches(mob)
                    ? MobHealthModifierClientConfig.showOnHostileMobs()
                    : MobHealthModifierClientConfig.showOnPassiveMobs();
        }

        // Porte-armures et autres entites vivantes sans comportement : pas de barre.
        return false;
    }

    /**
     * Construit la barre dans le repere de l'entite, orientee face a la camera.
     *
     * @param entity entite concernee
     * @param poseStack pile de matrices fournie par l'evenement
     * @param buffers source des tampons de rendu
     * @param partialTick avancement entre deux ticks, pour une orientation fluide
     */
    private static void renderBar(
            LivingEntity entity, PoseStack poseStack, MultiBufferSource buffers, float partialTick) {

        Vec3 anchor = entity.getAttachments()
                .getNullable(EntityAttachment.NAME_TAG, 0, entity.getViewYRot(partialTick));
        if (anchor == null) {
            return;
        }

        float fraction = Mth.clamp(entity.getHealth() / entity.getMaxHealth(), 0.0F, 1.0F);
        float width = MobHealthModifierClientConfig.getBarWidth();
        float height = MobHealthModifierClientConfig.getBarHeight();
        float halfWidth = width / 2.0F;

        poseStack.pushPose();
        poseStack.translate(
                anchor.x,
                anchor.y + 0.5D + MobHealthModifierClientConfig.getVerticalOffset(),
                anchor.z);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(SCALE, -SCALE, SCALE);

        Matrix4f matrix = poseStack.last().pose();

        // debugQuads : POSITION_COLOR, translucide, sans face culling et sans lightmap. La barre
        // reste donc lisible de nuit, tout en restant masquee par les blocs qui la precedent.
        VertexConsumer buffer = buffers.getBuffer(RenderType.debugQuads());

        float left = -halfWidth;
        float right = halfWidth;
        float top = -height;
        float bottom = 0.0F;
        float split = left + width * fraction;

        // Cadre dessine en quatre bandes plutot qu'en un rectangle place derriere la barre : aucun
        // quad ne se superpose, ce qui evite tout z-fighting entre surfaces coplanaires.
        float outerLeft = left - BORDER_THICKNESS;
        float outerRight = right + BORDER_THICKNESS;
        quad(buffer, matrix, outerLeft, top - BORDER_THICKNESS, outerRight, top, BORDER_COLOR);
        quad(buffer, matrix, outerLeft, bottom, outerRight, bottom + BORDER_THICKNESS, BORDER_COLOR);
        quad(buffer, matrix, outerLeft, top, left, bottom, BORDER_COLOR);
        quad(buffer, matrix, right, top, outerRight, bottom, BORDER_COLOR);

        if (fraction > 0.0F) {
            quad(buffer, matrix, left, top, split, bottom, healthColor(fraction));
        }
        if (fraction < 1.0F) {
            quad(buffer, matrix, split, top, right, bottom, EMPTY_COLOR);
        }

        poseStack.popPose();
    }

    /**
     * Couleur du remplissage, du rouge (mort imminente) au vert (pleine sante) en passant par le
     * jaune et l'orange.
     *
     * @param fraction part de vie restante, entre 0 et 1
     * @return couleur au format ARGB opaque
     */
    private static int healthColor(float fraction) {
        // La teinte 0 est rouge et la teinte 1/3 est verte dans le cercle chromatique.
        return Mth.hsvToArgb(fraction / 3.0F, 1.0F, 1.0F, 255);
    }

    /**
     * Empile un quad plan dans le repere local de la barre.
     *
     * @param buffer tampon de rendu
     * @param matrix matrice de transformation courante
     * @param x1 bord gauche
     * @param y1 bord haut
     * @param x2 bord droit
     * @param y2 bord bas
     * @param color couleur ARGB
     */
    private static void quad(
            VertexConsumer buffer, Matrix4f matrix, float x1, float y1, float x2, float y2, int color) {
        buffer.addVertex(matrix, x1, y2, 0.0F).setColor(color);
        buffer.addVertex(matrix, x2, y2, 0.0F).setColor(color);
        buffer.addVertex(matrix, x2, y1, 0.0F).setColor(color);
        buffer.addVertex(matrix, x1, y1, 0.0F).setColor(color);
    }
}
