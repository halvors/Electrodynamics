package resonantinduction.render;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import resonantinduction.ResonantInduction;
import resonantinduction.contractor.TileEntityEMContractor;
import resonantinduction.model.ModelEMContractor;

public class RenderEMContractor extends TileEntitySpecialRenderer
{
	public ModelEMContractor MODEL = new ModelEMContractor();
	public static final ResourceLocation TEXTURE = new ResourceLocation(ResonantInduction.DOMAIN, ResonantInduction.MODEL_TEXTURE_DIRECTORY + "em_contractor.png");
	public static final ResourceLocation TEXTURE_PUSH = new ResourceLocation(ResonantInduction.DOMAIN, ResonantInduction.MODEL_TEXTURE_DIRECTORY + "em_contractor_push.png");

	@Override
	public void renderTileEntityAt(TileEntity t, double x, double y, double z, float f)
	{
		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5, y + 1.5, z + 0.5);
		GL11.glRotatef(180F, 0.0F, 0.0F, 1.0F);
		GL11.glRotatef(90F, 0.0F, 1.0F, 0.0F);

		switch (((TileEntityEMContractor) t).getFacing())
		{
			case DOWN:
				GL11.glRotatef(180, 0, 0, 1);
				GL11.glTranslatef(0, -2, 0);
				break;
			case UP:
				break;
			case NORTH:
				GL11.glTranslatef(1, 1, 0);
				GL11.glRotatef(90, 0, 0, 1);
				break;
			case SOUTH:
				GL11.glTranslatef(-1, 1, 0);
				GL11.glRotatef(-90, 0, 0, 1);
				break;
			case WEST:
				GL11.glTranslatef(0, 1, 1);
				GL11.glRotatef(-90, 1, 0, 0);
				break;
			case EAST:
				GL11.glTranslatef(0, 1, -1);
				GL11.glRotatef(90, 1, 0, 0);
				break;
		}

		if (((TileEntityEMContractor) t).suck)
		{
			this.func_110628_a(TEXTURE);
		}
		else
		{
			this.func_110628_a(TEXTURE_PUSH);
		}
		
		MODEL.render(0.0625f, ((TileEntityEMContractor)t).canFunction() && ResonantInduction.proxy.isPaused());

		GL11.glPopMatrix();
	}
}
