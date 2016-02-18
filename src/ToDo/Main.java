package ToDo;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Scanner;

public class Main {
	
	Connection conn = null;
	PreparedStatement ps = null;
	ResultSet rs = null;
	String db = null;
	String host = null;
	
	public Main(String url) {
		String[] urlsplit = url.split("/");
		host = urlsplit[0];
		db = urlsplit[1];
		try {
			System.out.println("Trying to connect to database @ "+ url);
			conn = DriverManager.getConnection("jdbc:mysql://" + url +"?" + "user=root&password=comp6302&useSSL=false");
		} catch (SQLException ex) {
			System.out.println("Cannot connect to DB: "+ db);
			try {
				// connect to the mysql server and create the db
				conn = DriverManager.getConnection("jdbc:mysql://"+ host + "?" + "user=root&password=comp6302&useSSL=false");
				System.out.println("Going to try to create DB: "+db);
				ps = conn.prepareStatement("CREATE DATABASE "+ db);
				ps.execute();
				System.out.println("Created DB: "+ db + " sucessfully");
				conn.close();
			} catch (SQLException sqle) {
				System.out.println("Cannot create DB: "+ db +" on MySQL Server. Is it running? Exiting...");
				System.exit(1);
			}
		}
		try {
			conn = DriverManager.getConnection("jdbc:mysql://" + url +"?" + "user=root&password=comp6302&useSSL=false");
			System.out.println("Got Mysql database connection");
			//makes the table
			String sql = "CREATE TABLE `todo` (" +
						"`id` INT NOT NULL," +
						"`message` VARCHAR(255) NULL," +
						"`timestamp` TIMESTAMP NULL," +
						"PRIMARY KEY (`id`)," +
						" UNIQUE INDEX `id_UNIQUE` (`id` ASC));";
			ps = conn.prepareStatement(sql);
			try {
				ps.execute();
			}catch (SQLException se) {}
		}catch (SQLException sqle) {
			sqle.printStackTrace();
			System.out.println("Cannot create DB: "+ db +" on MySQL Server. Is it running?");
		}
	}
	public static boolean isInteger(String s) {
	    try { 
	        Integer.parseInt(s); 
	    } catch(NumberFormatException e) { 
	        return false; 
	    } catch(NullPointerException e) {
	        return false;
	    }
	    return true;
	}
	
	/* 
	 * "REQUEST" METHODS
	 */
	private void postToDo(int id, String message) throws SQLException {
		/*
		 * Stores the string ‘todo message’ in the database with the supplied integer ‘id’ and the client’s timestamp. 
		 * Overwrite any existing values.
		 */
		
		// check if already in the database
		String msg = getToDo(id);
		if (msg == null || msg.equals("")) {
			// insert if not
			ps = conn.prepareStatement("INSERT INTO todo(`id`, `message`, `timestamp`) VALUES (?,?,?)");
			ps.setInt(1, id);
			ps.setString(2, message);
			ps.setTimestamp(3, new Timestamp(new Date().getTime()));
			System.out.println("Inserted message: '"+message+"' successfully");
		}else {
			// update if it is
			ps = conn.prepareStatement("UPDATE todo SET `message`=?, `timestamp`=? WHERE `id`=?;");
			ps.setString(1, message);
			ps.setTimestamp(2, new Timestamp(new Date().getTime()));
			ps.setInt(3, id);
			System.out.println("Updated message: '"+message+"' successfully");
		}
		ps.executeUpdate(); //this works for insert and update
	}
	private String getToDo(int id) throws SQLException {
		/*
		 * Retrieves and displays the todo message to the console and when it was posted.
		 */
		ps = conn.prepareStatement("SELECT message FROM todo WHERE id = ?");
		ps.setInt(1, id);
		rs = ps.executeQuery();
		String message = "";
		while(rs.next()) {
			message = rs.getString("message").toString();
		}
		return message;
		
	}
	private ResultSet getAllToDos() throws SQLException {
		/*
		 * Retrieves a List of all todo messages as a map of <id,todo message> pairs and prints it to the console.
		 */
		ps = conn.prepareStatement("SELECT id, message FROM todo");
		return ps.executeQuery();		
	}
	private void deleteToDo(int id) throws SQLException {
		/*
		 * Deletes the todo message at the given id from the database.
		 */
		ps = conn.prepareStatement("DELETE FROM todo WHERE id = ?");
		ps.setInt(1, id );
		ps.executeUpdate();
		System.out.println("DELETED successfully");

	}
	private void replicateDB(String url) throws SQLException {
		/*
		 * Migrates the database from one mysql server to another. 
		 * The destination is at ‘URI’ in the form “[host]/[database name]”.
		 */
		rs = getAllToDos();
		Main newserver = new Main(url);
		while(rs.next()) {
			newserver.postToDo(rs.getInt("id"), rs.getString("message"));
		}
		System.out.println("Done transferring to " + url);
	}
	@SuppressWarnings("resource")
	public static void main(String[] args) throws SQLException {
		Main m = new Main("localhost/lab1_michaelhollister");
		Scanner sc = new Scanner(System.in);
		String input = "";
		
		 // Main loop
		
		while (true) {
			System.out.println("\nEnter a command...");
			input = sc.nextLine();
			String[] cmd = input.split("\\s+");
			String method = cmd[0].toUpperCase();
			if (method.equals("POST") && cmd.length >= 3 && isInteger(cmd[1])) {
				String message = "";
				for (int i=2;i<cmd.length;i++) {
					message += cmd[i];
					if (i != cmd.length-1) {
						message += " ";
					}
				}
				m.postToDo(Integer.parseInt(cmd[1]), message);
			}else if (method.equals("GET") && cmd.length == 1) {
				m.rs = m.getAllToDos();
				int count = 0;
				while(m.rs.next()) {
					System.out.println("<" + m.rs.getInt("id") + ">, <" + m.rs.getString("message") + ">");
					count++;
				}
				if (count == 0) {
					System.out.println("NO MESSAGES TO DISPLAY");
				}
			}else if (method.equals("GET") && cmd.length == 2 && isInteger(cmd[1])) {
				String message = m.getToDo(Integer.parseInt(cmd[1]));
				if (message == null || message.equals("")) {
					System.out.println("NO MESSAGE TO DISPLAY");
				}else {
					System.out.println("MESSAGE: '"+message+"'");
				}
			}else if (method.equals("DELETE") && cmd.length == 2 && isInteger(cmd[1])) {
				m.deleteToDo(Integer.parseInt(cmd[1]));
			}else if (method.equals("REPLICATE") && cmd.length == 2) {
				m.replicateDB(cmd[1]);
			}else {
				System.out.println("Error: Invalid Command");
			}
		}
	}
}
