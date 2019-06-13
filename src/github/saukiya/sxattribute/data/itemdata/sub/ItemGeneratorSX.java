package github.saukiya.sxattribute.data.itemdata.sub;

import github.saukiya.sxattribute.SXAttribute;
import github.saukiya.sxattribute.data.condition.SubCondition;
import github.saukiya.sxattribute.data.itemdata.ItemUpdate;
import github.saukiya.sxattribute.data.itemdata.SubItemGenerator;
import github.saukiya.sxattribute.util.CalculatorUtil;
import github.saukiya.sxattribute.util.Config;
import github.saukiya.sxattribute.verision.MaterialControl;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author Saukiya
 * @Date 2019/2/27 17:48
 */
public class ItemGeneratorSX implements SubItemGenerator, ItemUpdate {

    JavaPlugin plugin;

    String pathName;

    String key;

    ConfigurationSection config;

    String displayName;

    List<String> ids;

    List<String> loreList;

    List<String> enchantList;

    List<String> itemFlagList;

    boolean unbreakable;

    Color color;

    String skullName;

    int hashCode;

    boolean update;

    public ItemGeneratorSX(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private ItemGeneratorSX(String pathName, String key, ConfigurationSection config) {
        this.pathName = pathName;
        this.key = key;
        this.config = config;
        this.displayName = config.getString("Name");
        this.ids = config.isList("ID") ? config.getStringList("ID") : Collections.singletonList(config.getString(".ID"));
        this.loreList = config.getStringList("Lore");
        this.enchantList = config.getStringList("EnchantList");
        this.itemFlagList = config.getStringList("ItemFlagList");
        this.unbreakable = config.getBoolean("Unbreakable");
        this.color = config.getColor("Color");
        this.skullName = config.getString("SkullName");
        this.hashCode = config.getValues(true).hashCode();
        this.update = config.getBoolean("Update");
    }

    @Override
    public JavaPlugin getPlugin() {
        return plugin;
    }

    @Override
    public String getType() {
        return "SX";
    }

    @Override
    public SubItemGenerator newGenerator(String pathName, String key, ConfigurationSection config) {
        return new ItemGeneratorSX(pathName, key, config);
    }

    @Override
    public String getPathName() {
        return pathName;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getName() {
        return SXAttribute.getAPI().getRandomStringManager().processRandomString(displayName, new HashMap<>());
    }

    @Override
    public ConfigurationSection getConfig() {
        return config;
    }

    @Override
    public ItemStack getItem(Player player) {
        Map<String, String> lockRandomMap = new HashMap<>();
        String displayName = SXAttribute.getAPI().getRandomStringManager().processRandomString(this.displayName, lockRandomMap);
        String id = SXAttribute.getAPI().getRandomStringManager().processRandomString(ids.get(SXAttribute.getRandom().nextInt(ids.size())), lockRandomMap);
        List<String> loreList = new ArrayList<>();
        for (String lore : this.loreList) {
            lore = SXAttribute.getAPI().getRandomStringManager().processRandomString(lore, lockRandomMap);
            if (!lore.contains("%DeleteLore%")) {
                loreList.addAll(Arrays.asList(lore.replace("\n", "/n").split("/n")));
            }
        }
        if (SXAttribute.isPlaceholder() && player != null) {
            displayName = PlaceholderAPI.setPlaceholders(player, displayName);
            loreList = PlaceholderAPI.setPlaceholders(player, loreList);
        }
        // 计算器<c:>
        try {
            for (int i = 0; i < loreList.size(); i++) {
                String lore = loreList.get(i);
                List<String> replaceExprList = SXAttribute.getAPI().getRandomStringManager().getStringList("<c:", ">", lore);
                for (String expr : replaceExprList) {
                    lore = lore.replaceFirst("<c:" + expr.replace("*", "\\*").replace("(", "\\(").replace(")", "\\)").replace("+", "\\+") + ">", SXAttribute.getDf().format(CalculatorUtil.getResult(expr)));
                }
                loreList.set(i, lore);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<String> enchantList = new ArrayList<>();
        for (String enchant : this.enchantList) {
            enchant = SXAttribute.getAPI().getRandomStringManager().processRandomString(enchant, lockRandomMap);
            if (!enchant.contains("%DeleteLore%")) {
                enchantList.addAll(Arrays.asList(enchant.replace("\n", "/n").split("/n")));
            }
        }

        ItemStack item = getItemStack(displayName, id, loreList, enchantList, itemFlagList, unbreakable, color, skullName);
        if (lockRandomMap.size() > 0) {
            List<String> list = new ArrayList<>();
            for (Map.Entry<String, String> entry : lockRandomMap.entrySet()) {
                list.add(entry.getKey() + "§e§k|§e§r" + entry.getValue());
            }
            SXAttribute.getAPI().getNbtUtil().setNBTList(item, SXAttribute.getInst().getName() + "-Lock", list);
        }
        if (item.getItemMeta().hasLore()) {
            if (Config.isClearDefaultAttribute() && SXAttribute.getAPI().getNbtUtil().isEquipment(item) && config.getBoolean("ClearAttribute", true)) {
                SXAttribute.getAPI().getNbtUtil().clearAttribute(item);
            }
        }
        return item;
    }

    @Override
    public ConfigurationSection saveItem(ItemStack saveItem, ConfigurationSection config) {
        ItemMeta itemMeta = saveItem.getItemMeta();
        config.set("Name", itemMeta.hasDisplayName() ? itemMeta.getDisplayName().replace("§", "&") : null);
        //TODO
        config.set("ID", saveItem.getType().name() + (saveItem.getDurability() != 0 ? ":" + saveItem.getDurability() : ""));
        if (itemMeta.hasLore()) {
            config.set("Lore", itemMeta.getLore().stream().map(s -> s.replace("§", "&")).collect(Collectors.toList()));
        }
        if (itemMeta.hasEnchants()) {
            config.set("EnchantList", itemMeta.getEnchants().entrySet().stream().map(entry -> entry.getKey().getName() + ":" + entry.getValue()).collect(Collectors.toList()));
        }
        if (itemMeta.getItemFlags().size() > 0) {
            config.set("ItemFlagList", itemMeta.getItemFlags().stream().map(Enum::name).collect(Collectors.toList()));
        }
        config.set("Unbreakable", SubCondition.isUnbreakable(itemMeta));
        if (itemMeta instanceof LeatherArmorMeta) {
            config.set("Color", ((LeatherArmorMeta) itemMeta).getColor());
        }
        if (itemMeta instanceof SkullMeta) {
            config.set("SkullName", ((SkullMeta) itemMeta).getOwner());
        }
        return config;
    }


    /**
     * 快速生成物品
     *
     * @param itemName     String
     * @param id           String
     * @param loreList     List
     * @param itemFlagList List
     * @param unbreakable  Boolean
     * @param color        String
     * @param skullName    String
     * @return ItemStack
     */
    public ItemStack getItemStack(String itemName, String id, List<String> loreList, List<String> enchantList, List<String> itemFlagList, boolean unbreakable, Color color, String skullName) {

        ItemStack item = MaterialControl.fromString(id).parseItem();
        String[] idSplit = id.split(":");
        if (item.getType().getMaxDurability() != 0 && idSplit.length > 1) {
            item.setDurability(Short.parseShort(idSplit[1]));
        }

        ItemMeta meta = item.getItemMeta();

        if (itemName != null) {
            meta.setDisplayName(itemName.replace("&", "§"));
        }
        for (int i = 0; i < loreList.size(); i++) {
            loreList.set(i, loreList.get(i).replace("&", "§"));
        }
        meta.setLore(loreList);

        for (String enchant : enchantList) {
            String[] enchantSplit = enchant.split(":");
            Enchantment enchantment = Enchantment.getByName(enchantSplit[0]);
            int level = Integer.valueOf(enchantSplit[1]);
            if (enchantment != null && level != 0) {
                meta.addEnchant(enchantment, level, true);
            }
        }

        for (ItemFlag itemFlag : ItemFlag.values()) {
            if (itemFlagList.contains(itemFlag.name())) {
                meta.addItemFlags(itemFlag);
            }
        }

        if (SXAttribute.getVersionSplit()[1] >= 11) {
            meta.setUnbreakable(unbreakable);
        } else {
            meta.spigot().setUnbreakable(unbreakable);
        }

        if (color != null && meta instanceof LeatherArmorMeta) {
            ((LeatherArmorMeta) meta).setColor(color);
        }

        if (skullName != null && meta instanceof SkullMeta) {
            ((SkullMeta) meta).setOwner(skullName);
        }

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public int getHashCode() {
        return hashCode;
    }

    @Override
    public boolean isUpdate() {
        return update;
    }
}
