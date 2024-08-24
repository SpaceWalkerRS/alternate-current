package alternate.current.wire;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import alternate.current.AlternateCurrentMod;
import alternate.current.mixin.RegionWorldStorageAccessor;

import net.minecraft.world.storage.WorldStorage;

public class Config {

	static Config forLevel(WorldStorage storage) {
		return new Config(storage);
	}

	private final Path path;

	private boolean enabled = true;
	private UpdateOrder updateOrder = UpdateOrder.HORIZONTAL_FIRST_OUTWARD;

	private boolean modified;

	public Config(WorldStorage storage) {
		this.path = ((RegionWorldStorageAccessor) storage).alternate_current$getDirectory().toPath().resolve("alternate-current.conf");
	}

	public boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		AlternateCurrentMod.on = enabled;
		this.modified = true;
	}

	public UpdateOrder getUpdateOrder() {
		return updateOrder;
	}

	public void setUpdateOrder(UpdateOrder updateOrder) {
		this.updateOrder = Objects.requireNonNull(updateOrder);
		this.modified = true;
	}

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
