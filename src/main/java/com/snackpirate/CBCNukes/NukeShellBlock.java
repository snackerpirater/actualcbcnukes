package com.snackpirate.CBCNukes;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.CommonShellBigCannonProjectileProperties;
import rbasamoyai.createbigcannons.munitions.big_cannon.SimpleShellBlock;
import rbasamoyai.createbigcannons.munitions.config.PropertiesMunitionEntity;

import java.util.List;

public class NukeShellBlock extends SimpleShellBlock<CommonShellBigCannonProjectileProperties> {
	protected NukeShellBlock(Properties properties) {
		super(properties);
	}

	@Override
	public AbstractBigCannonProjectile<?> getProjectile(Level level, List<StructureTemplate.StructureBlockInfo> list) {
		NukeShellProjectile projectile = CBCNukes.NUKE_SHELL_PROJECTILE.get().create(level);
		projectile.setFuze(getFuze(list));
		return projectile;
	}

	@Override
	public EntityType<? extends PropertiesMunitionEntity<? extends CommonShellBigCannonProjectileProperties>> getAssociatedEntityType() {
		return CBCNukes.NUKE_SHELL_PROJECTILE.get();
	}

}
