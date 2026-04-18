package net.ledok.factory_ld.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ledok.factory_ld.config.FactoryLdConfig;
import net.ledok.factory_ld.client.power.PowerWireClientState;
import net.ledok.factory_ld.world.item.PowerLineToolItem;
import net.ledok.factory_ld.world.power.PowerConnectable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public final class PowerWireBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private static final int SEGMENTS = 24;
    private static final int COLOR_R = 40;
    private static final int COLOR_G = 40;
    private static final int COLOR_B = 44;
    private static final int COLOR_A = 255;
    private static final int PREVIEW_R = 110;
    private static final int PREVIEW_G = 210;
    private static final int PREVIEW_B = 255;
    private static final int PREVIEW_TOO_FAR_R = 230;
    private static final int PREVIEW_TOO_FAR_G = 60;
    private static final int PREVIEW_TOO_FAR_B = 60;
    private static final double WIRE_THICKNESS = 0.08;
    private static final double PREVIEW_AIM_DISTANCE = 20.0;

    public PowerWireBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!(blockEntity instanceof PowerConnectable connectable)) {
            return;
        }
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        BlockPos pos = blockEntity.getBlockPos();
        long packedPos = pos.asLong();
        Set<Long> links = PowerWireClientState.linksFor(level, packedPos);
        if (links.isEmpty() && !shouldRenderPreview(level, pos)) {
            return;
        }

        Vec3 startLocal = connectable.powerConnectionOffset();

        for (long otherPacked : links) {
            if (packedPos >= otherPacked) {
                continue;
            }
            BlockPos otherPos = BlockPos.of(otherPacked);
            BlockEntity otherEntity = level.getBlockEntity(otherPos);
            if (!(otherEntity instanceof PowerConnectable otherConnectable)) {
                continue;
            }
            Vec3 otherLocal = Vec3.atLowerCornerOf(otherPos.subtract(pos)).add(otherConnectable.powerConnectionOffset());
            VertexConsumer consumer = buffer.getBuffer(RenderType.debugQuads());
            drawWire(consumer, poseStack, pos, startLocal, otherLocal, COLOR_R, COLOR_G, COLOR_B, COLOR_A);
        }

        renderPreviewLine(blockEntity, connectable, poseStack, buffer);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    private static boolean shouldRenderPreview(Level level, BlockPos pos) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        PowerLineToolItem.PendingLink selection = getToolSelection(player);
        if (selection == null) {
            return false;
        }
        return selection.dimensionId().equals(level.dimension().location()) && selection.pos().equals(pos);
    }

    private static void renderPreviewLine(BlockEntity blockEntity, PowerConnectable connectable, PoseStack poseStack, MultiBufferSource buffer) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        PowerLineToolItem.PendingLink selection = getToolSelection(player);
        if (selection == null || !selection.dimensionId().equals(level.dimension().location()) || !selection.pos().equals(blockEntity.getBlockPos())) {
            return;
        }

        BlockHitResult hitResult = raycastForPreview(level, player);
        Vec3 targetWorld;
        boolean invalidConnectorTarget = false;
        if (hitResult != null) {
            targetWorld = hitResult.getLocation();
            BlockPos targetPos = hitResult.getBlockPos();
            BlockEntity targetEntity = level.getBlockEntity(targetPos);
            if (targetEntity instanceof PowerConnectable targetConnectable) {
                if (canMagnet(level, blockEntity.getBlockPos(), connectable, targetPos, targetConnectable)) {
                    targetWorld = Vec3.atLowerCornerOf(targetPos).add(targetConnectable.powerConnectionOffset());
                } else {
                    invalidConnectorTarget = true;
                }
            }
        } else {
            Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            targetWorld = cameraPos.add(player.getLookAngle().scale(PREVIEW_AIM_DISTANCE));
        }
        Vec3 targetLocal = targetWorld.subtract(blockEntity.getBlockPos().getX(), blockEntity.getBlockPos().getY(), blockEntity.getBlockPos().getZ());
        VertexConsumer consumer = buffer.getBuffer(RenderType.debugQuads());
        int maxDistance = Math.max(1, FactoryLdConfig.load().maxPowerLinkDistance());
        double previewDistanceSq = connectable.powerConnectionOffset().distanceToSqr(targetLocal);
        boolean tooFar = previewDistanceSq > (double) maxDistance * maxDistance;
        boolean renderRed = tooFar || invalidConnectorTarget;
        drawWire(
            consumer,
            poseStack,
            blockEntity.getBlockPos(),
            connectable.powerConnectionOffset(),
            targetLocal,
            renderRed ? PREVIEW_TOO_FAR_R : PREVIEW_R,
            renderRed ? PREVIEW_TOO_FAR_G : PREVIEW_G,
            renderRed ? PREVIEW_TOO_FAR_B : PREVIEW_B,
            COLOR_A
        );
    }

    private static boolean canMagnet(
        Level level,
        BlockPos startPos,
        PowerConnectable startNode,
        BlockPos targetPos,
        PowerConnectable targetNode
    ) {
        if (startPos.equals(targetPos)) {
            return false;
        }
        int maxDistance = Math.max(1, FactoryLdConfig.load().maxPowerLinkDistance());
        if (startPos.distSqr(targetPos) > (double) maxDistance * maxDistance) {
            return false;
        }
        Set<Long> startLinks = PowerWireClientState.linksFor(level, startPos.asLong());
        boolean alreadyLinked = startLinks.contains(targetPos.asLong());
        if (alreadyLinked) {
            return true;
        }
        Set<Long> targetLinks = PowerWireClientState.linksFor(level, targetPos.asLong());
        return startLinks.size() < startNode.maxPowerConnections() && targetLinks.size() < targetNode.maxPowerConnections();
    }

    private static PowerLineToolItem.PendingLink getToolSelection(LocalPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof PowerLineToolItem) {
            return PowerLineToolItem.getSelection(mainHand);
        }
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof PowerLineToolItem) {
            return PowerLineToolItem.getSelection(offHand);
        }
        return null;
    }

    private static BlockHitResult raycastForPreview(Level level, LocalPlayer player) {
        Vec3 from = player.getEyePosition(1.0F);
        Vec3 to = from.add(player.getLookAngle().scale(PREVIEW_AIM_DISTANCE));
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK ? hit : null;
    }

    private static void drawWire(VertexConsumer consumer, PoseStack poseStack, BlockPos origin, Vec3 from, Vec3 to, int r, int g, int b, int a) {
        Vec3 delta = to.subtract(from);
        float distance = (float) delta.length();
        float sag = Mth.clamp(distance * 0.06f, 0.05f, 1.1f);
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 cameraLocal = cameraPos.subtract(origin.getX(), origin.getY(), origin.getZ());
        PoseStack.Pose pose = poseStack.last();
        for (int i = 0; i < SEGMENTS; i++) {
            float t0 = (float) i / SEGMENTS;
            float t1 = (float) (i + 1) / SEGMENTS;
            Vec3 p0 = sagPoint(from, to, t0, sag);
            Vec3 p1 = sagPoint(from, to, t1, sag);
            Vec3 tangent = p1.subtract(p0).normalize();
            Vec3 mid = p0.add(p1).scale(0.5);
            Vec3 toCamera = cameraLocal.subtract(mid).normalize();
            Vec3 side = tangent.cross(toCamera);
            if (side.lengthSqr() < 1.0E-6) {
                side = tangent.cross(new Vec3(0.0, 1.0, 0.0));
                if (side.lengthSqr() < 1.0E-6) {
                    side = tangent.cross(new Vec3(1.0, 0.0, 0.0));
                }
            }
            side = side.normalize().scale(WIRE_THICKNESS * 0.5);

            Vec3 p0a = p0.add(side);
            Vec3 p0b = p0.subtract(side);
            Vec3 p1a = p1.add(side);
            Vec3 p1b = p1.subtract(side);

            consumer.addVertex(pose.pose(), (float) p0a.x, (float) p0a.y, (float) p0a.z).setColor(r, g, b, a);
            consumer.addVertex(pose.pose(), (float) p1a.x, (float) p1a.y, (float) p1a.z).setColor(r, g, b, a);
            consumer.addVertex(pose.pose(), (float) p1b.x, (float) p1b.y, (float) p1b.z).setColor(r, g, b, a);
            consumer.addVertex(pose.pose(), (float) p0b.x, (float) p0b.y, (float) p0b.z).setColor(r, g, b, a);
        }
    }

    private static Vec3 sagPoint(Vec3 from, Vec3 to, float t, float sag) {
        Vec3 linear = from.lerp(to, t);
        double drop = Math.sin(t * Math.PI) * sag;
        return linear.subtract(0.0, drop, 0.0);
    }
}
