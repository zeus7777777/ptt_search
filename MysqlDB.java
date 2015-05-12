package ptt_search;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MysqlDB
{
	Connection con;
	PreparedStatement ps;
	public MysqlDB() throws ClassNotFoundException, SQLException
	{
		Class.forName("com.mysql.jdbc.Driver");
		con = DriverManager.getConnection("jdbc:mysql://localhost:3306/DATA","root","km0049!!!");
	}
	public ResultSet query(String query)
	{
		System.out.print(query+"\n");
		try
		{
			ps = con.prepareStatement(query);
			return ps.executeQuery();
		} 
		catch (Exception ee)
		{
			System.out.println(ee.getMessage());
		}
		return null;
	}
}
