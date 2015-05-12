package ptt_search;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Scanner;

public class Test
{

	public static void main(String[] args) throws ClassNotFoundException, ParseException, SQLException
	{
		Scanner scan = new Scanner(System.in);
		PTTSearch ptts = new PTTSearch(scan.next());
		ptts.search();
	}
}
