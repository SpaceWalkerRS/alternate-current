package alternate.current.mixin;

import java.io.File;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.storage.RegionWorldStorage;

@Mixin(RegionWorldStorage.class)
public interface RegionWorldStorageAccessor {

	@Accessor("dir")
	File alternate_current$getDirectory();

}
