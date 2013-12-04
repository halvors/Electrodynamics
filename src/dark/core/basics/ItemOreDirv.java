package dark.core.basics;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.oredict.OreDictionary;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dark.api.events.LaserEvent;
import dark.core.interfaces.IExtraInfo.IExtraItemInfo;
import dark.core.prefab.ItemBasic;
import dark.core.prefab.ModPrefab;
import dark.machines.DarkMain;

/** A series of items that are derived from a basic material
 * 
 * @author DarkGuardsman */
public class ItemOreDirv extends ItemBasic implements IExtraItemInfo
{
    public ItemOreDirv()
    {
        super(ModPrefab.getNextItemId(), "Metal_Parts", DarkMain.CONFIGURATION);
        this.setHasSubtypes(true);
        this.setCreativeTab(CreativeTabs.tabMaterials);
    }

    @Override
    public String getUnlocalizedName(ItemStack itemStack)
    {
        if (itemStack != null)
        {
            return "item." + DarkMain.getInstance().PREFIX + EnumOrePart.getFullName(itemStack.getItemDamage());
        }
        else
        {
            return this.getUnlocalizedName();
        }
    }

    @Override
    public Icon getIconFromDamage(int i)
    {
        return EnumMaterial.getIcon(i);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerIcons(IconRegister iconRegister)
    {
        for (EnumMaterial mat : EnumMaterial.values())
        {
            mat.itemIcons = new Icon[EnumOrePart.values().length];
            for (EnumOrePart part : EnumOrePart.values())
            {
                if (mat.shouldCreateItem(part))
                {
                    mat.itemIcons[part.ordinal()] = iconRegister.registerIcon(DarkMain.getInstance().PREFIX + mat.simpleName + part.simpleName);
                }
            }
        }
    }

    @Override
    public void getSubItems(int par1, CreativeTabs par2CreativeTabs, List par3List)
    {
        for (EnumMaterial mat : EnumMaterial.values())
        {
            for (EnumOrePart part : EnumOrePart.values())
            {
                ItemStack stack = EnumMaterial.getStack(mat, part, 1);
                if (stack != null && mat.shouldCreateItem(part) && mat.itemIcons[part.ordinal()] != null)
                {
                    par3List.add(stack);
                }
            }
        }
    }

    @Override
    public boolean hasExtraConfigs()
    {
        return false;
    }

    @Override
    public void loadExtraConfigs(Configuration config)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void loadOreNames()
    {
        for (EnumMaterial mat : EnumMaterial.values())
        {
            for (EnumOrePart part : EnumOrePart.values())
            {
                ItemStack stack = EnumMaterial.getStack(mat, part, 1);
                if (stack != null && mat.shouldCreateItem(part) && mat.itemIcons[part.ordinal()] != null)
                {
                    OreDictionary.registerOre(EnumOrePart.getFullName(stack.getItemDamage()), stack);
                }
            }
        }

    }

    @ForgeSubscribe
    public void LaserSmeltEvent(LaserEvent.LaserDropItemEvent event)
    {
        if (event.items != null)
        {

            for (int i = 0; i < event.items.size(); i++)
            {
                if (event.items.get(i).itemID == Block.blockIron.blockID)
                {
                    event.items.set(i, EnumMaterial.getStack(EnumMaterial.IRON, EnumOrePart.MOLTEN, event.items.get(i).stackSize * 9));
                }
                else if (event.items.get(i).itemID == Block.blockGold.blockID)
                {
                    event.items.set(i, EnumMaterial.getStack(EnumMaterial.GOLD, EnumOrePart.MOLTEN, event.items.get(i).stackSize * 9));
                }
                else if (event.items.get(i).itemID == Block.oreIron.blockID)
                {
                    event.items.set(i, EnumMaterial.getStack(EnumMaterial.IRON, EnumOrePart.MOLTEN, event.items.get(i).stackSize));
                }
                else if (event.items.get(i).itemID == Block.oreGold.blockID)
                {
                    event.items.set(i, EnumMaterial.getStack(EnumMaterial.GOLD, EnumOrePart.MOLTEN, event.items.get(i).stackSize));
                }

                String oreName = OreDictionary.getOreName(OreDictionary.getOreID(event.items.get(i)));

                if (oreName != null)
                {
                    for (EnumMaterial mat : EnumMaterial.values())
                    {
                        if (oreName.equalsIgnoreCase("ore" + mat.simpleName) || oreName.equalsIgnoreCase(mat.simpleName + "ore"))
                        {
                            event.items.set(i, mat.getStack(EnumOrePart.MOLTEN, event.items.get(i).stackSize + 1 + event.world.rand.nextInt(3)));
                            break;
                        }
                        else if (oreName.equalsIgnoreCase("ingot" + mat.simpleName) || oreName.equalsIgnoreCase(mat.simpleName + "ingot"))
                        {
                            event.items.set(i, mat.getStack(EnumOrePart.MOLTEN, event.items.get(i).stackSize));
                            break;
                        }
                    }
                }
            }
        }
    }
}
