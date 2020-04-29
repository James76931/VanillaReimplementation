package net.minestom.vanilla.blockentity;

import net.minestom.server.effects.Effects;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.StackingRule;
import net.minestom.server.utils.BlockPosition;

import java.util.Random;

public class JukeboxBlockEntity extends BlockEntity {

    public static final String DISC_STACK = "disc";

    public JukeboxBlockEntity(BlockPosition position) {
        super(position);
        set(DISC_STACK, ItemStack.getAirItem(), ItemStack.class);
    }

    public boolean onInteract(Player player, Player.Hand hand, ItemStack heldItem) {
        ItemStack stack = get(DISC_STACK);
        if(stack.isAir()) {
            if(isMusicDisc(heldItem.getMaterial())) {
                set(DISC_STACK, heldItem.clone(), ItemStack.class);
                player.getInstance().refreshBlockId(getPosition(), Block.JUKEBOX.withProperties("has_record=true"));
                if(!player.isCreative()) {
                    StackingRule stackingRule = heldItem.getStackingRule();
                    ItemStack newUsedItem = stackingRule.apply(heldItem, stackingRule.getAmount(heldItem) - 1);

                    if (hand == Player.Hand.OFF) {
                        player.getInventory().setItemInOffHand(newUsedItem);
                    } else { // Main
                        player.getInventory().setItemInMainHand(newUsedItem);
                    }
                }
                player.getInstance().getPlayers().forEach(playerInInstance -> {
                    if(playerInInstance.getDistance(player) < 64) {
                        playerInInstance.playEffect(Effects.PLAY_RECORD, getPosition().getX(), getPosition().getY(), getPosition().getZ(), heldItem.getMaterial().getId(), false);
                    }
                });
                return true;
            }
        } else {
            stopPlayback(player.getInstance());
            set(DISC_STACK, ItemStack.getAirItem(), ItemStack.class);
            player.getInstance().refreshBlockId(getPosition(), Block.JUKEBOX.withProperties("has_record=false"));
            return true;
        }
        return false;
    }

    /**
     * Stops playback in an instance
     * @param instance
     */
    private void stopPlayback(Instance instance) {
        ItemEntity discEntity = new ItemEntity(get(DISC_STACK));
        discEntity.setPickable(true);

        Random rng = new Random();
        discEntity.getPosition().setX(getPosition().getX()+0.5f);
        discEntity.getPosition().setY(getPosition().getY()+1f);
        discEntity.getPosition().setZ(getPosition().getZ()+0.5f);

        final float horizontalSpeed = 2f;
        final float verticalSpeed = 5f;
        discEntity.getVelocity().setX((float) rng.nextGaussian()*horizontalSpeed);
        discEntity.getVelocity().setZ((float) rng.nextGaussian()*horizontalSpeed);

        discEntity.getVelocity().setY(rng.nextFloat()*verticalSpeed);

        instance.addEntity(discEntity);
        instance.getPlayers().forEach(playerInInstance -> {
            // stop playback
            playerInInstance.playEffect(Effects.PLAY_RECORD, getPosition().getX(), getPosition().getY(), getPosition().getZ(), -1, false);
        });
    }

    private boolean isMusicDisc(Material material) {
        return material.name().startsWith("MUSIC_DISC"); // TODO: better recognition than based on the name?
    }

    public void onDestroyed(Instance instance) {
        ItemStack stack = get(DISC_STACK);
        if(!stack.isAir()) {
            stopPlayback(instance);
        }
    }
}
