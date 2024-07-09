package com.snackpirate.CBCNukes;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.particle.ACParticleRegistry;
import com.github.alexmodguy.alexscaves.server.block.blockentity.NuclearSirenBlockEntity;
import com.github.alexmodguy.alexscaves.server.block.poi.ACPOIRegistry;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.item.NuclearExplosionEntity;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import com.google.common.base.Predicates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import rbasamoyai.createbigcannons.munitions.big_cannon.CommonShellBigCannonProjectileProperties;
import rbasamoyai.createbigcannons.munitions.big_cannon.FuzedBigCannonProjectile;

import java.util.stream.Stream;

public class NukeShellProjectile extends FuzedBigCannonProjectile<CommonShellBigCannonProjectileProperties> {
	private int countdownTicks;
	private static final EntityDataAccessor<Boolean> ACTIVATED = SynchedEntityData.defineId(NukeShellProjectile.class, EntityDataSerializers.BOOLEAN);
	protected NukeShellProjectile(EntityType type, Level level) {
		super(type, level);
		this.countdownTicks = 100;
	}

	//a delay between "detonation" (fuze activation) and actual explosion would be good to match AC i think
	@Override
	protected void detonate() {
		setActivated(true);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(ACTIVATED, false);
	}

	private boolean isActivated() {
		return this.entityData.get(ACTIVATED);
	}
	private void setActivated(boolean activated) {
		this.entityData.set(ACTIVATED, activated);
	}

	//copied from alex's caves, refuse to believe it works
	private void nukeKaboom() {
		NuclearExplosionEntity explosion = (NuclearExplosionEntity)((EntityType) ACEntityRegistry.NUCLEAR_EXPLOSION.get()).create(this.level());
		explosion.copyPosition(this);
		explosion.setSize(((Double) AlexsCaves.COMMON_CONFIG.nukeExplosionSizeModifier.get()).floatValue());
		this.level().addFreshEntity(explosion);
		this.discard();
	}
	@Override
	public BlockState getRenderedBlockState() {
		return CBCNukes.NUKE_SHELL.getDefaultState().setValue(BlockStateProperties.FACING, Direction.NORTH);
	}

	//activates nearby sirens, courtesy of alexs caves
	//also does the cool particles
	@Override
	protected void onTickRotate() {
		super.onTickRotate();
		if (level() instanceof ServerLevel server && tickCount % 10 == 0) {
			getNearbySirens(server, 256).forEach(
					pos -> {
						if (server.getBlockEntity(pos) instanceof NuclearSirenBlockEntity siren)
							siren.setNearestNuclearBomb(this);
			});
			if (countdownTicks <= 0) {
				this.nukeKaboom();
			} else if (isActivated()) {
				countdownTicks-=10;
			}
//			CBCNukes.LOGGER.info("countdown {}", countdownTicks);
		}
		boolean b = random.nextFloat() < 0.5f;
		CBCNukes.LOGGER.info("client {}, activated {}, random {}", level().isClientSide, isActivated(), b);
		if (level().isClientSide && isActivated() && b) {
			Vec3 center = this.getEyePosition();
			level().addParticle(ACParticleRegistry.PROTON.get(), center.x, center.y, center.z, center.x, center.y, center.z);
		}
	}

	//force delay between activation and explosion, needs to alwas be able to sit in ground
	@Override
	public boolean canLingerInGround() {
		return true;
	}

	private Stream<BlockPos> getNearbySirens(ServerLevel world, int range) {
		PoiManager poiManager = world.getPoiManager();
		return poiManager.findAll(poiTypeHolder -> poiTypeHolder.is(ACPOIRegistry.NUCLEAR_SIREN.getKey()), Predicates.alwaysTrue(), this.blockPosition(), range, PoiManager.Occupancy.ANY);
	}

	//same defusal behavior as AC nukes
	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		CBCNukes.LOGGER.info("interact {}", player.getItemInHand(hand).is(Tags.Items.SHEARS));
		if (player.getItemInHand(hand).is(Tags.Items.SHEARS)) {
			player.swing(hand);
			this.playSound(ACSoundRegistry.NUCLEAR_BOMB_DEFUSE.get());;
			this.remove(RemovalReason.KILLED);
			this.spawnAtLocation(new ItemStack(CBCNukes.NUKE_SHELL.asItem()));
			if (!player.isCreative()) player.getItemInHand(hand).hurtAndBreak(1, player, e -> e.broadcastBreakEvent(hand));
		}
		return InteractionResult.SUCCESS;
	}

//	@Override
//	public InteractionResult interactAt(Player p_19980_, Vec3 p_19981_, InteractionHand p_19982_) {
//		return interact(p_19980_, p_19982_);
//	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public boolean mayInteract(Level p_150167_, BlockPos p_150168_) {
		return true;
	}


	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.countdownTicks = tag.getInt("countdownTicks");
		setActivated(tag.getBoolean("activated"));
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("countdownTicks", this.countdownTicks);
		tag.putBoolean("activated", isActivated());
	}
}
