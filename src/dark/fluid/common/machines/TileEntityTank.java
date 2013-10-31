package dark.fluid.common.machines;

import dark.core.prefab.tilenetwork.NetworkTileEntities;
import dark.core.prefab.tilenetwork.fluid.NetworkFluidContainers;
import dark.core.prefab.tilenetwork.fluid.NetworkPipes;
import dark.fluid.common.prefab.TileEntityFluidNetworkTile;

public class TileEntityTank extends TileEntityFluidNetworkTile
{

    @Override
    public NetworkFluidContainers getTileNetwork()
    {
        if (!(this.network instanceof NetworkFluidContainers))
        {
            this.setTileNetwork(new NetworkFluidContainers(this));
        }
        return (NetworkFluidContainers) this.network;
    }

    @Override
    public void setTileNetwork(NetworkTileEntities network)
    {
        if (network instanceof NetworkFluidContainers)
        {
            this.network = (NetworkFluidContainers) network;
        }
    }
}
