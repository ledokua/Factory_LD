package net.ledok.factory_ld.world.item;

import net.ledok.factory_ld.config.FactoryLdConfig;
import net.ledok.factory_ld.world.power.PowerConnectable;
import net.ledok.factory_ld.world.power.PowerNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class PowerLineToolItem extends Item {
    private static final String TAG_LINK_SELECTION = "PowerLinkSelection";
    private static final String TAG_DIMENSION = "Dimension";
    private static final String TAG_POS_X = "X";
    private static final String TAG_POS_Y = "Y";
    private static final String TAG_POS_Z = "Z";
    private static final double AIM_DISTANCE = 20.0;

    public PowerLineToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.CONSUME;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.CONSUME;
        }
        return handleServerUse(player, context.getItemInHand(), context.getClickedPos());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.consume(stack);
        }
        BlockPos targetPos = findConnectorTarget(level, serverPlayer);
        if (targetPos != null) {
            handleServerUse(serverPlayer, stack, targetPos);
        } else if (serverPlayer.isShiftKeyDown() && getSelection(stack) != null) {
            clearSelection(stack);
            serverPlayer.displayClientMessage(Component.translatable("item.factory_ld.power_line_tool.cleared"), true);
        }
        return InteractionResultHolder.consume(stack);
    }

    private InteractionResult handleServerUse(ServerPlayer player, ItemStack stack, BlockPos clickedPos) {
        Level level = player.level();
        if (player.isShiftKeyDown()) {
            if (getSelection(stack) != null) {
                clearSelection(stack);
                player.displayClientMessage(Component.translatable("item.factory_ld.power_line_tool.cleared"), true);
            }
            return InteractionResult.CONSUME;
        }

        if (!(level.getBlockEntity(clickedPos) instanceof PowerConnectable connectable)) {
            // Ignore non-connectable right-clicks so the tool does not trigger normal block interactions.
            return InteractionResult.CONSUME;
        }

        PendingLink pending = getSelection(stack);
        if (pending == null || !pending.dimensionId().equals(level.dimension().location())) {
            setSelection(stack, level, clickedPos);
            player.displayClientMessage(
                Component.translatable(
                    "item.factory_ld.power_line_tool.selected",
                    clickedPos.toShortString(),
                    PowerNetworkManager.connectionCount(level, clickedPos),
                    connectable.maxPowerConnections()
                ),
                true
            );
            return InteractionResult.CONSUME;
        }

        BlockPos firstPos = pending.pos();
        if (firstPos.equals(clickedPos)) {
            clearSelection(stack);
            player.displayClientMessage(Component.translatable("item.factory_ld.power_line_tool.cleared"), true);
            return InteractionResult.CONSUME;
        }

        PowerNetworkManager.ToggleResult result = PowerNetworkManager.toggleConnection(level, firstPos, clickedPos);
        clearSelection(stack);
        switch (result) {
            case CONNECTED -> player.displayClientMessage(Component.translatable("item.factory_ld.power_line_tool.connected"), true);
            case DISCONNECTED -> player.displayClientMessage(Component.translatable("item.factory_ld.power_line_tool.disconnected"), true);
            case TOO_FAR -> player.displayClientMessage(
                Component.translatable("item.factory_ld.power_line_tool.too_far", FactoryLdConfig.load().maxPowerLinkDistance()),
                true
            );
            case INVALID -> player.displayClientMessage(Component.translatable("item.factory_ld.power_line_tool.invalid"), true);
        }
        return InteractionResult.CONSUME;
    }

    private static BlockPos findConnectorTarget(Level level, ServerPlayer player) {
        Vec3 from = player.getEyePosition(1.0F);
        Vec3 to = from.add(player.getLookAngle().scale(AIM_DISTANCE));
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        return level.getBlockEntity(pos) instanceof PowerConnectable ? pos : null;
    }

    public static PendingLink getSelection(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        CompoundTag root = customData.copyTag();
        if (!root.contains(TAG_LINK_SELECTION, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag selection = root.getCompound(TAG_LINK_SELECTION);
        if (!selection.contains(TAG_DIMENSION, Tag.TAG_STRING)
            || !selection.contains(TAG_POS_X, Tag.TAG_INT)
            || !selection.contains(TAG_POS_Y, Tag.TAG_INT)
            || !selection.contains(TAG_POS_Z, Tag.TAG_INT)) {
            return null;
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(selection.getString(TAG_DIMENSION));
        if (dimensionId == null) {
            return null;
        }
        return new PendingLink(
            dimensionId,
            new BlockPos(selection.getInt(TAG_POS_X), selection.getInt(TAG_POS_Y), selection.getInt(TAG_POS_Z))
        );
    }

    private static void setSelection(ItemStack stack, Level level, BlockPos pos) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag selection = new CompoundTag();
            selection.putString(TAG_DIMENSION, level.dimension().location().toString());
            selection.putInt(TAG_POS_X, pos.getX());
            selection.putInt(TAG_POS_Y, pos.getY());
            selection.putInt(TAG_POS_Z, pos.getZ());
            root.put(TAG_LINK_SELECTION, selection);
        });
    }

    private static void clearSelection(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> root.remove(TAG_LINK_SELECTION));
    }

    public record PendingLink(ResourceLocation dimensionId, BlockPos pos) {
    }
}
