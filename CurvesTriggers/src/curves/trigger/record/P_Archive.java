package curves.trigger.record;

import java.sql.SQLException;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import com.mysql.jdbc.PreparedStatement;

import curves.main.Bot;
import curves.main.DatabaseFactory;
import curves.trigger.IPeriodicHandler;

public class P_Archive implements IPeriodicHandler {

	int skip = 0;
	Logger log = Logger.getLogger(P_Archive.class);

	public boolean isReady(Bot bot, Hashtable<String, Object> storage) {
		skip = (skip + 1) % 1440;
		return skip == 1;
	}

	public void process(Bot bot, Hashtable<String, Object> storage) {
		PreparedStatement ps = null;
		try {
			ps = (PreparedStatement) bot
					.getDB()
					.prepareStatement(
							"INSERT IGNORE INTO " + DatabaseFactory.prefix_ + "curvesmessages_archive SELECT * FROM messages"
									+ " WHERE date( time ) < subdate( curdate( ) , INTERVAL 2 month );");
			ps.executeUpdate();
			ps = (PreparedStatement) bot
					.getDB()
					.prepareStatement(
							"DELETE FROM " + DatabaseFactory.prefix_ + "curvesmessages"
									+ " WHERE date( time ) < subdate( curdate( ) , INTERVAL 2 month );");
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			log.error("Unable archive messages.", e);
		}
	}

}
