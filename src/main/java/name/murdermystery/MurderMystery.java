package main.java.name.murdermystery;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.level.Level;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import com.creeperface.nukkit.placeholderapi.api.PlaceholderAPI;
import main.java.name.murdermystery.api.Api;
import main.java.name.murdermystery.listener.PlayerGame;
import main.java.name.murdermystery.listener.PlayerJoinAndQuit;
import main.java.name.murdermystery.listener.RoomLevelProtection;
import main.java.name.murdermystery.room.Room;
import main.java.name.murdermystery.utils.SetRoomConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MurderMystery
 * @author lt_name
 */
public class MurderMystery extends PluginBase {

    private static MurderMystery murderMystery;
    private Config config;
    private LinkedHashMap<String, Config> roomConfigs = new LinkedHashMap<>();
    private LinkedHashMap<String, Room> rooms = new LinkedHashMap<>();
    private LinkedHashMap<Integer, Skin> skins = new LinkedHashMap<>();

    public static MurderMystery getInstance() { return murderMystery; }

    @Override
    public void onEnable() {
        if (murderMystery == null) {
            murderMystery = this;
        }
        this.config = new Config(getDataFolder() + "/config.yml", 2);
        getServer().getPluginManager().registerEvents(new PlayerJoinAndQuit(), this);
        getServer().getPluginManager().registerEvents(new RoomLevelProtection(), this);
        getServer().getPluginManager().registerEvents(new PlayerGame(), this);
        File file1 = new File(this.getDataFolder() + "/Rooms");
        File file2 = new File(this.getDataFolder() + "/PlayerInventory");
        File file3 = new File(this.getDataFolder() + "/Skins");
        if (!file1.exists() && !file1.mkdirs()) {
            getLogger().error("Rooms 文件夹初始化失败");
        }
        if (!file2.exists() && !file2.mkdirs()) {
            getLogger().error("PlayerInventory 文件夹初始化失败");
        }
        if (!file3.exists() && !file3.mkdirs()) {
            getLogger().error("Skins 文件夹初始化失败");
        }
        getLogger().info("§a开始加载房间");
        this.loadRooms();
        getLogger().info("§a开始加载皮肤");
        this.loadSkins();
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            PlaceholderAPI api = PlaceholderAPI.getInstance();
            api.visitorSensitivePlaceholder("MurderPlayerMode", (player, placeholderParameters) -> Api.getPlayerMode(player), 20, true);
            api.visitorSensitivePlaceholder("MurderTime", (player, placeholderParameters) -> Api.getTime(player), 20, true);
            api.visitorSensitivePlaceholder("MurderSurvivorNumber", (player, placeholderParameters) -> Api.getSurvivor(player), 20, true);
            api.visitorSensitivePlaceholder("MurderRoomMode", (player, placeholderParameters) -> Api.getRoomMode(player), 20, true);
        }
        getLogger().info("§a插件加载完成！");
    }

    @Override
    public void onDisable() {
        this.config.save();
        if (this.rooms.values().size() > 0) {
            Iterator<Map.Entry<String, Room>> it = this.rooms.entrySet().iterator();
            while(it.hasNext()){
                Map.Entry<String, Room> entry = it.next();
                entry.getValue().endGame();
                getLogger().info("§c房间：" + entry.getKey() + " 已卸载！");
                it.remove();
            }
        }
        this.rooms.clear();
        this.roomConfigs.clear();
        this.skins.clear();
        getLogger().info("§c已卸载！");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equals("killer")) {
            if (sender instanceof Player) {
                Player player = ((Player) sender).getPlayer();
                if (args.length >0) {
                    switch (args[0]) {
                        case "join": case "加入":
                            for (Room room : this.rooms.values()) {
                                if (room.isPlaying(player)) {
                                    sender.sendMessage("§c你已经在一个房间中了!");
                                    return true;
                                }
                            }
                            for(Entity entity : player.getLevel().getEntities()) {
                                if (entity.isPassenger(player)) {
                                    sender.sendMessage("§a请勿在骑乘状态下进入房间！");
                                    return true;
                                }
                            }
                            if (args.length == 2) {
                                if (args[1] != null && this.rooms.containsKey(args[1])) {
                                    Room room = this.rooms.get(args[1]);
                                    if (room.getMode() == 2 || room.getMode() == 3) {
                                        sender.sendMessage("§a该房间正在游戏中，请稍后");
                                    }else if (room.getPlayers().values().size() > 15) {
                                        sender.sendMessage("§a该房间已满人，请稍后");
                                    } else {
                                        room.joinRoom(player);
                                    }
                                }else {
                                    sender.sendMessage("§a该房间不存在！");
                                }
                            }else {
                                sender.sendMessage("§a查看帮助：/killer help");
                            }
                            break;
                        case "quit": case "退出":
                            for (Room gameRoom : this.rooms.values()) {
                                if (gameRoom.getPlayers().containsKey(player)) {
                                    gameRoom.quitRoom(player, true);
                                }
                            }
                            sender.sendMessage("§a你已退出房间");
                            break;
                        case "list": case "列表":
                            StringBuilder list = new StringBuilder().append("§e房间列表： §a");
                            for (String string : this.rooms.keySet()) {
                                list.append(string).append(" ");
                            }
                            sender.sendMessage(String.valueOf(list));
                            break;
                        default:
                            sender.sendMessage("§e/killer--命令帮助");
                            sender.sendMessage("§a/killer join 房间名称 §e加入游戏");
                            sender.sendMessage("§a/killer quit §e退出游戏");
                            sender.sendMessage("§a/killer list §e查看房间列表");
                            break;
                    }
                }else {
                    sender.sendMessage("§a/killer help §e查看帮助");
                }
            }else {
                sender.sendMessage("请在游戏内输入！");
            }
            return true;
        }else if (command.getName().equals("kadmin")) {
            if (sender instanceof Player) {
                Player player = ((Player) sender).getPlayer();
                if (args.length > 0) {
                    switch (args[0]) {
                        case "设置出生点": case "setspawn": case "SetSpawn":
                            SetRoomConfig.setSpawn(player, getRoomConfig(player.getLevel()));
                            sender.sendMessage("§a出生点设置成功！");
                            break;
                        case "添加金锭生成点": case "addGoldSpawn":
                            SetRoomConfig.addGoldSpawn(player, getRoomConfig(player.getLevel()));
                            sender.sendMessage("§a金锭生成点添加成功！");
                            break;
                        case "设置金锭产出间隔":
                            if (args.length == 2) {
                                SetRoomConfig.setGoldSpawnTime(Integer.valueOf(args[1]), getRoomConfig(player.getLevel()));
                                sender.sendMessage("§a金锭产出间隔已设置为：" + Integer.valueOf(args[1]));
                            }else {
                                sender.sendMessage("§a查看帮助：/kadmin help");
                            }
                            break;
                        case "设置等待时间":
                            if (args.length == 2) {
                                SetRoomConfig.setWaitTime(Integer.valueOf(args[1]), getRoomConfig(player.getLevel()));
                                sender.sendMessage("§a等待时间已设置为：" + Integer.valueOf(args[1]));
                            }else {
                                sender.sendMessage("§a查看帮助：/kadmin help");
                            }
                            break;
                        case "设置游戏时间":
                            if (args.length == 2) {
                                if (Integer.parseInt(args[1]) > 60) {
                                    SetRoomConfig.setGameTime(Integer.valueOf(args[1]), getRoomConfig(player.getLevel()));
                                    sender.sendMessage("§a游戏时间已设置为：" + Integer.valueOf(args[1]));
                                }else {
                                    sender.sendMessage("§a游戏时间最小不能低于1分钟！");
                                }
                            }else {
                                sender.sendMessage("§a查看帮助：/kadmin help");
                            }
                            break;
                        case "reload": case "重载":
                            this.reLoadRooms();
                            sender.sendMessage("§a配置重载完成！");
                            break;
                        default:
                            sender.sendMessage("§e killer管理--命令帮助");
                            sender.sendMessage("§a/kadmin 设置出生点 §e设置当前位置为游戏出生点");
                            sender.sendMessage("§a/kadmin 添加金锭生成点 §e将当前位置设置为金锭生成点");
                            sender.sendMessage("§a/kadmin 设置金锭产出间隔 数字 §e设置金锭生成间隔");
                            sender.sendMessage("§a/kadmin 设置等待时间 数字 §e设置游戏人数足够后的等待时间");
                            sender.sendMessage("§a/kadmin 设置游戏时间 数字 §e设置每轮游戏最长时间");
                            sender.sendMessage("§a/kadmin reload §e重载所有房间");
                            break;
                    }
                }else {
                    sender.sendMessage("§a/kadmin help §e查看帮助");
                }
            }else {
                sender.sendMessage("§a请在游戏内输入！");
            }
            return true;
        }
        return false;
    }

    public Config getConfig() {
        return this.config;
    }

    public boolean getActionBar() {
        return this.config.getBoolean("底部显示信息", true);
    }

    public LinkedHashMap<String, Room> getRooms() {
        return this.rooms;
    }

    private Config getRoomConfig(Level level) {
        return getRoomConfig(level.getName());
    }

    private Config getRoomConfig(String level) {
        if (this.roomConfigs.containsKey(level)) {
            return this.roomConfigs.get(level);
        }
        if (!new File(getDataFolder() + "/Rooms/" + level + ".yml").exists()) {
            saveResource("room.yml", "/Rooms/" + level + ".yml", false);
        }
        Config config = new Config(getDataFolder() + "/Rooms/" + level + ".yml", 2);
        this.roomConfigs.put(level, config);
        return config;
    }

    public LinkedHashMap<Integer, Skin> getSkins() {
        return this.skins;
    }

    /**
     * 加载所有房间
     */
    private void loadRooms() {
        File[] s = new File(getDataFolder() + "/Rooms").listFiles();
        if (s != null) {
            for (File file1 : s) {
                String[] fileName = file1.getName().split("\\.");
                if (fileName.length > 0) {
                    Room room = new Room(getRoomConfig(fileName[0]));
                    this.rooms.put(fileName[0], room);
                    getLogger().info("§a房间：" + fileName[0] + " 已加载！");
                }
            }
        }
    }

    /**
     * 重载所有房间
     */
    private void reLoadRooms() {
        if (this.rooms.values().size() > 0) {
            Iterator<Map.Entry<String, Room>> it = this.rooms.entrySet().iterator();
            while(it.hasNext()){
                Map.Entry<String, Room> entry = it.next();
                entry.getValue().endGame();
                getLogger().info("§c房间：" + entry.getKey() + " 已卸载！");
                it.remove();
            }
        }
        if (this.roomConfigs.values().size() > 0) {
            this.roomConfigs.clear();
        }
        this.loadRooms();
    }

    /**
     * 加载所有皮肤
     */
    private void loadSkins() {
        File[] files = (new File(getDataFolder() + "/Skins")).listFiles();
        if (files != null && files.length > 0) {
            int x = 0;
            for (File file : files) {
                String skinName = file.getName();
                File skinFile = new File(getDataFolder() + "/Skins/" + skinName + "/skin.png");
                if (skinFile.exists()) {
                    Skin skin = new Skin();
                    BufferedImage skinData = null;
                    try {
                        skinData = ImageIO.read(skinFile);
                    } catch (IOException e) {
                        System.out.println(skinName + "加载失败");
                    }
                    if (skinData != null) {
                        skin.setSkinData(skinData);
                        skin.setSkinId(skinName);
                        getLogger().info("编号： " + x + " 皮肤： " + skinName + " 已加载");
                        this.skins.put(x, skin);
                        x++;
                    }else {
                        getLogger().info(skinName + "加载失败，这可能不是一个正确的图片");
                    }
                } else {
                    getLogger().info(skinName + "加载失败，请将皮肤文件命名为 skin.png");
                }
            }
        }
    }


}
