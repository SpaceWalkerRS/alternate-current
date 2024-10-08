package alternate.current.wire;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IServerLevel;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;

public interface Config {

	static Config forLevel(ServerLevel level, LevelStorageAccess storage) {
		if (level.dimension() == Level.OVERWORLD) {
			return new Primary(storage);
		} else {
			return new Derived(((IServerLevel) level.getServer().overworld()).alternate_current$getWireHandler().getConfig());
		}
	}

	boolean getEnabled();

	void setEnabled(boolean enabled);

	UpdateOrder getUpdateOrder();

	void setUpdateOrder(UpdateOrder updateOrder);

	void load();

	void save(boolean silent);

	public static class Primary implements Config {

		private final Path path;

		private boolean enabled = true;
		private UpdateOrder updateOrder = UpdateOrder.HORIZONTAL_FIRST_OUTWARD;

		private boolean modified;

		public Primary(LevelStorageAccess storage) {
			this.path = storage.getLevelPath(LevelResource.ROOT).resolve("alternate-current.conf");
		}

		@Override
		public boolean getEnabled() {
			return enabled;
		}

		@Override
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
			AlternateCurrentMod.on = enabled;
			this.modified = true;
		}

		@Override
		public UpdateOrder getUpdateOrder() {
			return updateOrder;
		}

		@Override
		public void setUpdateOrder(UpdateOrder updateOrder) {
			this.updateOrder = Objects.requireNonNull(updateOrder);
			this.modified = true;
		}

		@Override
		public void load() {
			if (Files.exists(path)) {
				try (BufferedReader br = Files.newBufferedReader(path)) {
					String line;

					while ((line = br.readLine()) != null) {
						if (!line.startsWith("#")) {
							String[] parts = line.split("[=]");

							if (parts.length == 2) {
								String key = parts[0];
								String value = parts[1];

								try {
									switch (key) {
									case "enabled":
										setEnabled(Boolean.parseBoolean(value));
										break;
									case "update-order":
										setUpdateOrder(UpdateOrder.byId(value));
										break;
									default:
										AlternateCurrentMod.LOGGER.info("skipping unknown option \'" + key + "\' in Alternate Current config");
									}
								} catch (Exception e) {
									AlternateCurrentMod.LOGGER.info("skipping bad value \'" + value + "\' for option \'" + key + "\' in Alternate Current config!", e);
								}
							}
						}
					}

					modified = false;
				} catch (IOException e) {
					AlternateCurrentMod.LOGGER.info("unable to load Alternate Current config!", e);
					modified = true;
				}
			} else {
				modified = true;
			}
		}

		@Override
		public void save(boolean silent) {
			if (modified) {
				if (!silent) {
					AlternateCurrentMod.LOGGER.info("saving Alternate Current config");
				}

				try (BufferedWriter bw = Files.newBufferedWriter(path)) {
					bw.write("enabled");
					bw.write('=');
					bw.write(Boolean.toString(enabled));
					bw.newLine();

					bw.write("update-order");
					bw.write('=');
					bw.write(updateOrder.id());
					bw.newLine();
				} catch (IOException e) {
					AlternateCurrentMod.LOGGER.info("unable to save Alternate Current config!", e);
				} finally {
					modified = false;
				}
			}
		}
	}

	public static class Derived implements Config {

		private final Config delegate;

		public Derived(Config delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean getEnabled() {
			return delegate.getEnabled();
		}

		@Override
		public void setEnabled(boolean enabled) {
			delegate.setEnabled(enabled);
		}

		@Override
		public UpdateOrder getUpdateOrder() {
			return delegate.getUpdateOrder();
		}

		@Override
		public void setUpdateOrder(UpdateOrder updateOrder) {
			delegate.setUpdateOrder(updateOrder);
		}

		@Override
		public void load() {
		}

		@Override
		public void save(boolean silent) {
		}
	}
}
