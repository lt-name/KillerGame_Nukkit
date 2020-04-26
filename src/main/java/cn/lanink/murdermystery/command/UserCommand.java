package cn.lanink.murdermystery.command;

import cn.lanink.murdermystery.MurderMystery;
import cn.lanink.murdermystery.room.Room;
import cn.lanink.murdermystery.ui.GuiCreate;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;

public class UserCommand extends Command {

    MurderMystery murderMystery = MurderMystery.getInstance();
    public final String name;

    public UserCommand(String name) {
        super(name, "MurderMystery 游戏命令", "/" + name + " help");
        this.name = name;
        this.setPermission("MurderMystery.all");
    }

    @Override
    public boolean execute(CommandSender commandSender, String label, String[] strings) {
        if (commandSender instanceof Player) {
            Player player = ((Player) commandSender).getPlayer();
            if (strings.length > 0) {
                switch (strings[0]) {
                    case "join": case "加入":
                        if (murderMystery.getRooms().size() > 0) {
                            for (Room room : murderMystery.getRooms().values()) {
                                if (room.isPlaying(player)) {
                                    commandSender.sendMessage("§c你已经在一个房间中了!");
                                    return true;
                                }
                            }
                            if (player.riding != null) {
                                commandSender.sendMessage("§a请勿在骑乘状态下进入房间！");
                                return true;
                            }
                            if (strings.length < 2) {
                                for (Room room : murderMystery.getRooms().values()) {
                                    if (room.getMode() == 0 || room.getMode() == 1) {
                                        room.joinRoom(player);
                                        commandSender.sendMessage("§a已为你随机分配房间！");
                                        return true;
                                    }
                                }
                            }else if (murderMystery.getRooms().containsKey(strings[1])) {
                                Room room = murderMystery.getRooms().get(strings[1]);
                                if (room.getMode() == 2 || room.getMode() == 3) {
                                    commandSender.sendMessage("§a该房间正在游戏中，请稍后");
                                }else if (room.getPlayers().values().size() > 15) {
                                    commandSender.sendMessage("§a该房间已满人，请稍后");
                                } else {
                                    room.joinRoom(player);
                                }
                                return true;
                            }else {
                                commandSender.sendMessage("§a该房间不存在！");
                                return true;
                            }
                        }
                        commandSender.sendMessage("§a暂无房间可用！");
                        return true;
                    case "quit": case "退出":
                        for (Room room : murderMystery.getRooms().values()) {
                            if (room.isPlaying(player)) {
                                room.quitRoom(player, true);
                                commandSender.sendMessage("§a你已退出房间");
                                return true;
                            }
                        }
                        commandSender.sendMessage("§a你本来就不在游戏房间！");
                        return true;
                    case "list": case "列表":
                        StringBuilder list = new StringBuilder().append("§e房间列表： §a");
                        for (String string : murderMystery.getRooms().keySet()) {
                            list.append(string).append(" ");
                        }
                        commandSender.sendMessage(String.valueOf(list));
                        return true;
                    default:
                        commandSender.sendMessage("§eMurderMystery--命令帮助");
                        commandSender.sendMessage("§a/" + name + " §e打开ui");
                        commandSender.sendMessage("§a/" + name + " join 房间名称 §e加入游戏");
                        commandSender.sendMessage("§a/" + name + " quit §e退出游戏");
                        commandSender.sendMessage("§a/" + name + " list §e查看房间列表");
                        return true;
                }
            }else {
                GuiCreate.sendUserMenu(player);
                return true;
            }
        }else {
            commandSender.sendMessage("请在游戏内输入！");
            return true;
        }
    }

}
