package resonantinduction.atomic.machine.accelerator;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotFurnace;
import net.minecraft.item.ItemStack;
import resonant.lib.gui.ContainerBase;
import resonantinduction.atomic.AtomicContent;

/**
 * Accelerator container
 */
public class ContainerAccelerator extends ContainerBase
{
	private TileAccelerator tileEntity;

	public ContainerAccelerator(EntityPlayer player, TileAccelerator tileEntity)
	{
		super(player, (IInventory) tileEntity);
		this.tileEntity = tileEntity;
		// Inputs
		addSlotToContainer(new Slot((IInventory) tileEntity, 0, 132, 26));
		addSlotToContainer(new Slot((IInventory) tileEntity, 1, 132, 51));
		// Output
		addSlotToContainer(new SlotFurnace(player, (IInventory) tileEntity, 2, 132, 75));
		addSlotToContainer(new SlotFurnace(player, (IInventory) tileEntity, 3, 106, 75));
		addPlayerInventory(player);
	}

	/**
	 * Called to transfer a stack from one inventory to the other eg. when shift clicking.
	 */
	@Override
	public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int par1)
	{
		ItemStack var2 = null;
		Slot var3 = (Slot) this.inventorySlots.get(par1);

		if (var3 != null && var3.getHasStack())
		{
			ItemStack itemStack = var3.getStack();
			var2 = itemStack.copy();

			if (par1 > 2)
			{
				if (itemStack.getItem() == AtomicContent.itemCell())
				{
					if (!this.mergeItemStack(itemStack, 1, 2, false))
					{
						return null;
					}
				}
				else if (!this.mergeItemStack(itemStack, 0, 1, false))
				{
					return null;
				}
			}
			else if (!this.mergeItemStack(itemStack, 3, 36 + 3, false))
			{
				return null;
			}

			if (itemStack.stackSize == 0)
			{
				var3.putStack((ItemStack) null);
			}
			else
			{
				var3.onSlotChanged();
			}

			if (itemStack.stackSize == var2.stackSize)
			{
				return null;
			}

			var3.onPickupFromSlot(par1EntityPlayer, itemStack);
		}

		return var2;
	}
}
