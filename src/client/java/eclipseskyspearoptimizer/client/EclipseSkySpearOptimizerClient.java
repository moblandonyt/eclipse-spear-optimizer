package eclipseskyspearoptimizer.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.lwjgl.glfw.GLFW;

/**
 * Eclipse Sky Spear Optimizer
 * Press the keybind (default: Right Shift) to swap to a Lunge spear in your
 * hotbar, attack with it, and swap back to the slot you were on.
 */
public class EclipseSkySpearOptimizerClient implements ClientModInitializer {

    public static final String MOD_ID = "eclipse-sky-spear-optimizer";

    private static KeyMapping optimizerKey;

    // 0 = idle, 1 = waiting for slot sync, 2 = attack, 3 = swap back
    private static int phase = 0;
    private static int returnSlot = -1;
    private static int spearSlot = -1;

    @Override
    public void onInitializeClient() {
        optimizerKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key." + MOD_ID + ".activate",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            LocalPlayer player = mc.player;
            if (player == null || mc.gameMode == null) {
                phase = 0;
                return;
            }
            Inventory inv = player.getInventory();

            switch (phase) {
                case 0 -> {
                    if (!optimizerKey.consumeClick()) return;

                    spearSlot = findLungeSpear(inv);
                    returnSlot = inv.getSelectedSlot();
                    if (spearSlot == -1 || spearSlot == returnSlot) return;

                    inv.setSelectedSlot(spearSlot);
                    phase = 1;
                }
                case 1 -> phase = 2; // let the server register the slot change
                case 2 -> {
                    KeyMapping.click(mc.options.keyAttack.getDefaultKey());
                    phase = 3;
                }
                case 3 -> {
                    inv.setSelectedSlot(returnSlot);
                    phase = 0;
                }
            }
        });
    }

    private static int findLungeSpear(Inventory inv) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.is(ItemTags.SPEARS)) continue;

            for (Holder<Enchantment> enchantment : stack.getEnchantments().keySet()) {
                if (enchantment.is(Enchantments.LUNGE)) return i;
            }
        }
        return -1;
    }
}
