package de.whitescan.playerplot.plot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.whitescan.playerplot.util.Friend;
import de.whitescan.playerplot.util.LocationParts;
import de.whitescan.playerplot.util.PlotPoint;
import de.whitescan.playerplot.util.storage.DatabaseConnection;

public class PlotDatabase {

	private static DatabaseConnection databaseConnection = DatabaseConnection.getInstance();

	public PlotDatabase() {
		initialize();
	}

	public void initialize() {
		try {
			databaseConnection.openConnection();
			Statement statement = databaseConnection.getConnection().createStatement();
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS PPlot (" + "uuid CHAR(36)," + "name VARCHAR(20),"
					+ "ownerID CHAR(36)," + "ownerName VARCHAR(16)," + "minX INT," + "minZ INT," + "maxX INT,"
					+ "maxZ INT," + "world VARCHAR(36)," + "components SMALLINT," + "PRIMARY KEY (uuid)" + ");");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS PSpawn (" + "plotId CHAR(36),"
					+ "worldName VARCHAR(36)," + "x DOUBLE," + "y DOUBLE," + "z DOUBLE," + "yaw FLOAT," + "pitch FLOAT,"
					+ "PRIMARY KEY (plotId)" + ");");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS PTrusts (" + "plotID CHAR(36)," + "friendID CHAR(36),"
					+ "friendName VARCHAR(16)," + "PRIMARY KEY (plotID, friendID) " + ");");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<Plot> fetchPlots() {
		List<Plot> plots = new ArrayList<>();
		try {
			databaseConnection.openConnection();
			ResultSet plotResults = databaseConnection.getConnection().createStatement()
					.executeQuery("SELECT * FROM PPlot;");
			while (plotResults.next()) {
				Plot plot = extractPlot(plotResults);
				plots.add(plot);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return plots;
	}

	private Plot extractPlot(ResultSet plotResults) throws SQLException {
		UUID plotID = UUID.fromString(plotResults.getString("uuid"));
		String name = plotResults.getString("name");
		UUID ownerID = UUID.fromString(plotResults.getString("ownerID"));
		String ownerName = plotResults.getString("ownerName");
		int minX = plotResults.getInt("minX");
		int minZ = plotResults.getInt("minZ");
		PlotPoint min = new PlotPoint(minX, minZ);
		int maxX = plotResults.getInt("maxX");
		int maxZ = plotResults.getInt("maxZ");
		PlotPoint max = new PlotPoint(maxX, maxZ);
		String world = plotResults.getString("world");
		int components = plotResults.getInt("components");
		Statement statement = databaseConnection.getConnection().createStatement();
		ResultSet friendsResult = statement.executeQuery("SELECT * FROM PTrusts WHERE plotID = '" + plotID + "';");
		List<Friend> friends = new ArrayList<>();
		while (friendsResult.next()) {
			UUID friendID = UUID.fromString(friendsResult.getString("friendID"));
			String friendName = friendsResult.getString("friendName");
			friends.add(new Friend(friendID, friendName));
		}

		ResultSet spawnResult = statement.executeQuery("SELECT * FROM PSpawn WHERE plotID = '" + plotID + "';");
		LocationParts locationParts = null;
		if (spawnResult.next()) {
			String worldName = spawnResult.getString("worldName");
			double x = spawnResult.getDouble("x");
			double y = spawnResult.getDouble("y");
			double z = spawnResult.getDouble("z");
			float yaw = spawnResult.getFloat("yaw");
			float pitch = spawnResult.getFloat("pitch");
			locationParts = new LocationParts(worldName, x, y, z, yaw, pitch);
		}

		return new Plot(plotID, name, ownerID, ownerName, min, max, world, components, friends, locationParts);
	}

	public void storePlot(UUID plotID, Plot plot) {
		try {
			databaseConnection.openConnection();
			Statement statement = databaseConnection.getConnection().createStatement();
			if (plot != null) {
				String name = plot.getName();
				UUID ownerID = plot.getOwnerID();
				String ownerName = plot.getOwnerName();
				PlotPoint min = plot.getMinCorner();
				int minX = min.getX();
				int minZ = min.getZ();
				PlotPoint max = plot.getMaxCorner();
				int maxX = max.getX();
				int maxZ = max.getZ();
				String world = plot.getWorld();
				int components = plot.getComponents();
				statement.executeUpdate(
						"REPLACE INTO PPlot (uuid, name, ownerID, ownerName, minX, minZ, maxX, maxZ, world, components) "
								+ "VALUES ('" + plotID + "', '" + name + "', '" + ownerID + "', '" + ownerName + "', "
								+ minX + ", " + minZ + ", " + maxX + ", " + maxZ + ", '" + world + "', " + components
								+ ");");
				List<Friend> friends = plot.getFriends();
				statement.executeUpdate("DELETE FROM PTrusts WHERE plotID = '" + plotID + "'");
				for (Friend friend : friends) {
					statement.executeUpdate("INSERT INTO PTrusts (plotID, friendID, friendName) " + "VALUES ('" + plotID
							+ "','" + friend.getUuid() + "', '" + friend.getName() + "');");
				}
				LocationParts spawn = plot.getSpawnParts();
				if (spawn != null) {
					statement.executeUpdate("REPLACE INTO PSpawn (plotId, worldName, x, y, z, yaw, pitch) "
							+ "VALUES ('" + plotID + "', '" + spawn.getWorldName() + "', '" + spawn.getX() + "', '"
							+ spawn.getY() + "', '" + spawn.getZ() + "', '" + spawn.getYaw() + "', '" + spawn.getPitch()
							+ "');");
				} else {
					statement.executeUpdate("DELETE FROM PSpawn WHERE plotId = '" + plotID + "'");
				}
			} else {
				statement.executeUpdate("DELETE FROM PTrusts WHERE plotID = '" + plotID + "'");
				statement.executeUpdate("DELETE FROM PPlot WHERE uuid = '" + plotID + "'");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
