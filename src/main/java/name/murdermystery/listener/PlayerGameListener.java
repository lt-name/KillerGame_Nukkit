package name.murdermystery.listener;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.entity.EntityDamageByChildEntityEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityShootBowEvent;
import cn.nukkit.event.inventory.InventoryPickupItemEvent;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerItemConsumeEvent;
import cn.nukkit.event.player.PlayerItemHeldEvent;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.potion.Effect;
import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.scheduler.Task;
import name.murdermystery.MurderMystery;
import name.murdermystery.event.MurderPlayerDamageEvent;
import name.murdermystery.room.Room;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * 游戏监听器（nk事件）
 * @author lt_name
 */
public class PlayerGameListener implements Listener {

    /**
     * 实体受到另一实体伤害事件
     * @param event 事件
     */
    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Level level = event.getDamager().getLevel();
        if (level == null || !MurderMystery.getInstance().getRooms().containsKey(level.getName()) ||
                MurderMystery.getInstance().getRooms().get(level.getName()).getMode() != 2) {
            return;
        }
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player player1 = (Player) event.getDamager();
            Player player2 = (Player) event.getEntity();
            if (player1 == null || player2 == null) {
                return;
            }
            Room room = MurderMystery.getInstance().getRooms().get(player1.getLevel().getName());
            if (room.isPlaying(player1) &&
                    room.getPlayerMode(player1) == 3 &&
                    player1.getInventory().getItemInHand().getCustomName().equals("§c杀手之剑") &&
                    room.isPlaying(player2) &&
                    room.getPlayerMode(player2) != 0) {
                Server.getInstance().getPluginManager().callEvent(new MurderPlayerDamageEvent(room, player1, player2));
            }
        }
        event.setCancelled();
    }

    /**
     * 实体受到另一个子实体伤害事件
     * @param event 事件
     */
    @EventHandler
    public void onDamageByChild(EntityDamageByChildEntityEvent event) {
        if (event.getDamager() == null || event.getEntity() == null ||
                event.getChild() == null) {
            return;
        }
        Level level = event.getDamager().getLevel();
        if (level == null || !MurderMystery.getInstance().getRooms().containsKey(level.getName()) ||
                MurderMystery.getInstance().getRooms().get(level.getName()).getMode() != 2) {
            return;
        }
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player player1 = ((Player) event.getDamager()).getPlayer();
            Player player2 = ((Player) event.getEntity()).getPlayer();
            if (player1 == player2) {
                return;
            }
            Room room = MurderMystery.getInstance().getRooms().get(player1.getLevel().getName());
            if (room.getPlayerMode(player1) != 3 && room.getPlayerMode(player1) != 0) {
                Server.getInstance().getPluginManager().callEvent(new MurderPlayerDamageEvent(room, player1, player2));
            }
        }
        event.setCancelled();
    }

    /**
     * 生命实体射出箭 事件
     * @param event 事件
     */
    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = ((Player) event.getEntity()).getPlayer();
            if (player == null) {
                return;
            }
            String levelName = player.getLevel().getName();
            if (!MurderMystery.getInstance().getRooms().containsKey(levelName) ||
                    MurderMystery.getInstance().getRooms().get(levelName).getMode() != 2) {
                return;
            }
/*            if (player.getInventory().getItemInHand().getCustomName().equals("§e侦探之弓")) {
                player.getInventory().addItem(Item.get(262, 0, 1));
                return;
            }*/
            if (MurderMystery.getInstance().getRooms().get(levelName).getPlayerMode(player) == 2) {
                player.getInventory().addItem(Item.get(262, 0, 1));
                return;
            }
            //回收平民的弓
            Server.getInstance().getScheduler().scheduleDelayedTask(new Task() {
                @Override
                public void onRun(int i) {
                    int j = 0; //箭的数量
                    boolean bow = false;
                    for (Item item : player.getInventory().getContents().values()) {
                        if (item.getId() == 262) {
                            j += item.getCount();
                            continue;
                        }
                        if (item.getId() == 261) {
                            bow = true;
                        }
                    }
                    if (j < 1 && bow) {
                        player.getInventory().removeItem(Item.get(261, 0, 1));
                    }
                }
            }, 20, true);
        }
    }

    /**
     * 收起掉落的物品时
     * @param event 事件
     */
    @EventHandler
    public void onPickupItem(InventoryPickupItemEvent event) {
        Level level = event.getItem().getLevel();
        if (level == null || !MurderMystery.getInstance().getRooms().containsKey(level.getName()) ||
                MurderMystery.getInstance().getRooms().get(level.getName()).getMode() != 2) {
            return;
        }
        if (event.getInventory() != null && event.getInventory() instanceof PlayerInventory) {
            Player player = (Player) event.getInventory().getHolder();
            Room room = MurderMystery.getInstance().getRooms().get(level.getName());
            if (event.getItem().getItem().getCustomName().equals("§e侦探之弓")) {
                if (room.getPlayerMode(player) != 1) {
                    event.setCancelled();
                    return;
                }
                room.addPlaying(player, 2);
                Server.getInstance().getScheduler().scheduleAsyncTask(MurderMystery.getInstance(), new AsyncTask() {
                    @Override
                    public void onRun() {
                        int j = 0; //箭的数量
                        for (Item item : player.getInventory().getContents().values()) {
                            if (item.getId() == 262) {
                                j += item.getCount();
                            }
                        }
                        if (j < 1) {
                            player.getInventory().addItem(Item.get(262, 0, 1));
                        }
                    }
                });
            }
        }
    }

    /**
     * 发送消息事件
     * @param event 事件
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String string = event.getMessage();
        if (player == null || string == null || string.startsWith("/")) {
            return;
        }
        Level level = player.getLevel();
        if (level == null || !MurderMystery.getInstance().getRooms().containsKey(level.getName())) {
            return;
        }
        Room room = MurderMystery.getInstance().getRooms().get(player.getLevel().getName());
        if (room.getMode() == 2 && room.getPlayerMode(player) == 0) {
            for (Player p : room.getPlayers().keySet()) {
                if (room.getPlayerMode(p) == 0) {
                    p.sendMessage("§c[死亡] " + player.getName() + "§b >>> " + string);
                }
            }
            event.setCancelled(true);
            event.setMessage(" ");
        }
    }

    /**
     * 玩家手持物品事件
     * @param event 事件
     */
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (event == null) {
            return;
        }
        Player player = event.getPlayer();
        Item item = event.getItem();
        if (player == null || item == null || item.getNamedTag() == null) {
            return;
        }
        Level level = player.getLevel();
        if (level == null || !MurderMystery.getInstance().getRooms().containsKey(level.getName()) ||
                MurderMystery.getInstance().getRooms().get(level.getName()).getMode() != 2) {
            return;
        }
        Room room = MurderMystery.getInstance().getRooms().get(level.getName());
        CompoundTag tag = item.getNamedTag();
        if (room.isPlaying(player) && room.getPlayerMode(player) == 3) {
            if (tag.getBoolean("isMurderItem") && tag.getInt("MurderType") == 1) {
                if (room.effectCD < 1) {
                    Effect effect = Effect.getEffect(1);
                    effect.setAmplifier(2);
                    effect.setVisible(false);
                    effect.setDuration(5);
                    player.addEffect(effect);
                    room.effectCD = 10;
                }
            }else if (player.getEffects().containsValue(Effect.getEffect(1))) {
                player.removeAllEffects();
            }
        }
    }

    /**
     * 玩家点击事件
     * @param event 事件
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (player == null || block == null) {
            return;
        }
        Level level = player.getLevel();
        if (level == null || !MurderMystery.getInstance().getRooms().containsKey(level.getName())) {
            return;
        }
        if (event.getAction() == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK && !player.isOp()) {
            event.setCancelled(true);
            player.setAllowModifyWorld(false);
        }
        if (MurderMystery.getInstance().getRooms().get(level.getName()).getMode() == 2) {
            if (block.getId() == 118 &&
                    block.getLevel().getBlock(block.getFloorX(), block.getFloorY() - 1, block.getFloorZ()).getId() == 138) {
                Server.getInstance().getScheduler().scheduleAsyncTask(MurderMystery.getInstance(), new AsyncTask() {
                    @Override
                    public void onRun() {
                        int x = 0; //金锭数量
                        for (Item item : player.getInventory().getContents().values()) {
                            if (item.getId() == 266) {
                                x += item.getCount();
                            }
                        }
                        if (x > 0) {
                            player.getInventory().removeItem(Item.get(266, 0, 1));
                            Item item = Item.get(373, 0, 1);
                            item.setNamedTag(new CompoundTag()
                                    .putBoolean("isMurderItem", true)
                                    .putInt("MurderType", 21));
                            item.setCustomName("§a神秘药水");
                            item.setLore("未知效果的药水", "究竟是会带来好运，还是厄运？", "使用方法：直接饮用即可");
                            player.getInventory().addItem(item);
                            player.sendMessage("§a成功兑换到一瓶神秘药水！");
                        }else {
                            player.sendMessage("§a需要使用金锭兑换药水！");
                        }
                    }
                });
                event.setCancelled(true);
            }else if (block.getId() == 116 &&
                    block.getLevel().getBlock(block.getFloorX(), block.getFloorY() - 1, block.getFloorZ()).getId() == 169) {
                Server.getInstance().getScheduler().scheduleAsyncTask(MurderMystery.getInstance(), new AsyncTask() {
                    @Override
                    public void onRun() {
                        int x = 0; //金锭数量
                        boolean notHave = true;
                        for (Item item : player.getInventory().getContents().values()) {
                            if (item.getId() == 266) {
                                x += item.getCount();
                                continue;
                            }
                            if (item.getCustomName().equals("§a护盾生成器")) {
                                notHave = false;
                            }
                        }
                        if (x > 0) {
                            if (notHave) {
                                player.getInventory().removeItem(Item.get(266, 0, 1));
                                Item item = Item.get(241, 3, 1);
                                item.setNamedTag(new CompoundTag()
                                        .putBoolean("isMurderItem", true)
                                        .putInt("MurderType", 22));
                                item.setCustomName("§a护盾生成器");
                                item.setLore("可以生成一面短时间存在的墙", "它的功能很差，但却能在关键时间救你一命", "使用方法：放在地面即可");
                                player.getInventory().addItem(item);
                                player.sendMessage("§a成功兑换到一个护盾！");
                            }else {
                                player.sendMessage("§a你只能携带一个护盾！");
                            }
                        }else {
                            player.sendMessage("§a需要使用金锭兑换护盾！");
                        }
                    }
                });
                event.setCancelled(true);
            }
        }
    }

    /**
     * 玩家使用消耗品事件
     * @param event 事件
     */
    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        Item item = event.getItem();
        if (player == null || item == null || item.getNamedTag() == null) {
            return;
        }
        Level level = player.getLevel();
        if (level == null || !MurderMystery.getInstance().getRooms().containsKey(level.getName()) ||
                MurderMystery.getInstance().getRooms().get(level.getName()).getMode() != 2) {
            return;
        }
        CompoundTag tag = item.getNamedTag();
        Room room = MurderMystery.getInstance().getRooms().get(player.getLevel().getName());
        if (room.isPlaying(player) &&
                tag.getBoolean("isMurderItem") &&
                tag.getInt("MurderType") == 21) {
            if (room.getPlayerMode(player) == 3) {
                Effect effect = Effect.getEffect(2);
                effect.setDuration(100);
                player.addEffect(effect);
            }else {
                int random = new Random().nextInt(100);
                Effect effect = null;
                if (random < 100 && random >= 70) {
                    effect = Effect.getEffect(1); //速度
                }else if (random < 70 && random >= 60) {
                    effect = Effect.getEffect(16); //夜视
                }else if (random < 60 && random >= 50) {
                    effect = Effect.getEffect(14); //隐身
                }else if (random < 50 && random >= 30) {
                    effect = Effect.getEffect(8); //跳跃提升2
                    effect.setAmplifier(2);
                }else if (random < 30 && random >= 10) {
                    effect = Effect.getEffect(2); //缓慢
                }
                if (effect != null) {
                    effect.setDuration(100);
                    player.addEffect(effect);
                }
            }
        }
    }

    /**
     * 方块放置事件
     * @param event 事件
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Item item = event.getItem();
        Block block = event.getBlockReplace();
        if (player == null || item == null || block == null) {
            return;
        }
        Level level = player.getLevel();
        if (level == null || !MurderMystery.getInstance().getRooms().containsKey(level.getName())) {
            return;
        }
        CompoundTag tag = item.getNamedTag();
        if (MurderMystery.getInstance().getRooms().get(level.getName()).getMode() == 2 &&
                tag.getBoolean("isMurderItem") &&
                tag.getInt("MurderType") == 22) {
            level.addSound(block, Sound.RANDOM_ANVIL_USE);
            //>315 <45  X
            //>135 <225 X
            double yaw = player.getYaw();
            Server.getInstance().getScheduler().scheduleAsyncTask(MurderMystery.getInstance(), new AsyncTask() {
                @Override
                public void onRun() {
                    ArrayList<Vector3> blockList = new ArrayList<>();
                    blockList.add(block);
                    for (int y = block.getFloorY() ; y < (block.getFloorY() + 6); y++) {
                        if ((yaw > 315 || yaw < 45) || (yaw > 135 && yaw < 225)) {
                            for (int x = block.getFloorX() ; x < (block.getFloorX() + 4); x++) {
                                Vector3 vector3 = new Vector3(x, y, block.getFloorZ());
                                if (level.getBlock(vector3).getId() == 0) {
                                    level.setBlock(vector3, Block.get(241, 3));
                                    blockList.add(vector3);
                                }
                            }
                            for (int x = block.getFloorX() ; x > (block.getFloorX() - 4); x--) {
                                Vector3 vector3 = new Vector3(x, y, block.getFloorZ());
                                if (level.getBlock(vector3).getId() == 0) {
                                    level.setBlock(vector3, Block.get(241, 3));
                                    blockList.add(vector3);
                                }
                            }
                        }else {
                            for (int z = block.getFloorZ() ; z < (block.getFloorZ() + 4); z++) {
                                Vector3 vector3 = new Vector3(block.getFloorX(), y, z);
                                if (level.getBlock(vector3).getId() == 0) {
                                    level.setBlock(vector3, Block.get(241, 3));
                                    blockList.add(vector3);
                                }
                            }
                            for (int z = block.getFloorZ() ; z > (block.getFloorZ() - 4); z--) {
                                Vector3 vector3 = new Vector3(block.getFloorX(), y, z);
                                if (level.getBlock(vector3).getId() == 0) {
                                    level.setBlock(vector3, Block.get(241, 3));
                                    blockList.add(vector3);
                                }
                            }
                        }
                    }
                    Server.getInstance().getScheduler().scheduleDelayedTask(new Task() {
                        @Override
                        public void onRun(int i) {
                            Iterator<Vector3> it = blockList.iterator();
                            while (it.hasNext()) {
                                level.setBlock(it.next(), Block.get(0));
                                it.remove();
                            }
                        }
                    }, 100);
                }
            });
        }else if (!player.isOp()) {
            event.setCancelled(true);
        }
    }

}