package ptt_search;

import main.KafkaTopicConsumer;

public class Run
{
	public static void main(String[] args)
	{
		KafkaTopicConsumer ktc = new KafkaTopicConsumer(
				"192.168.1.104:2181", 
				"PTT", 
				"ptt_request", 
				new KafkaListener()
		);
	}
}
