package ptt_search;

import main.MsgReceiver;

public class KafkaListener extends MsgReceiver
{
	PTTSearch ptts;
	public void execute(String message)
	{
		try
		{
			ptts = new PTTSearch(message);
			ptts.search();
		}
		catch(Exception ee)
		{
			System.out.print(ee.getMessage());
		}
	}
}
