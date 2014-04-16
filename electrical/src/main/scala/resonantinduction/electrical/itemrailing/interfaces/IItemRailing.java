package resonantinduction.electrical.itemrailing.interfaces;

import calclavia.lib.grid.INodeProvider;
import calclavia.lib.render.EnumColor;
import net.minecraftforge.common.ForgeDirection;
import universalelectricity.api.vector.VectorWorld;

/**
 * implement on Part Railings.
 *
 * @since 16/03/14
 * @author tgame14
 */
public interface IItemRailing extends INodeProvider
{
    public boolean canItemEnter (IItemRailingTransfer item);

    public boolean canConnectToRailing (IItemRailing railing, ForgeDirection to);

    public EnumColor getRailingColor ();

    public IItemRailing setRailingColor (EnumColor color);

	public VectorWorld getWorldPos();



}