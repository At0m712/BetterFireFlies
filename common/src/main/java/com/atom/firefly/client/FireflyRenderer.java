package com.atom.firefly.client;

import com.atom.firefly.Constants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class FireflyRenderer extends EntityRenderer<FireflyEntity, FireflyRenderer.FireflyRenderState> {
    private final FireflyModel model;

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/entity/firefly_texture.png");
    private static final Identifier GLOW_TEXTURE = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/entity/firefly_glow.png");

    private static final RenderType SOLID_RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);
    private static final RenderType GLOW_RENDER_TYPE = RenderTypes.eyes(GLOW_TEXTURE);

    public FireflyRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new FireflyModel(context.bakeLayer(FireflyModel.LAYER_LOCATION));
    }

    @Override
    public FireflyRenderState createRenderState() {
        return new FireflyRenderState();
    }

    @Override
    public void extractRenderState(FireflyEntity entity, FireflyRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.ageInTicks = entity.tickCount + partialTick;
        state.yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
    }

    @Override
    public void submit(FireflyRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();

        poseStack.scale(0.3F, 0.3F, 0.3F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot));

        this.model.setupAnim(state);

        collector.submitModel(this.model, state, poseStack, SOLID_RENDER_TYPE, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, null, EntityRenderState.NO_OUTLINE, null);

        collector.submitModel(this.model, state, poseStack, GLOW_RENDER_TYPE, 15728880, OverlayTexture.NO_OVERLAY, -1, null, EntityRenderState.NO_OUTLINE, null);

        poseStack.popPose();

        super.submit(state, poseStack, collector, camera);
    }

    public static class FireflyRenderState extends EntityRenderState {
        public float ageInTicks;
        public float yRot;
    }
}