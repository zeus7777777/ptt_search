package ptt_search;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Article
{
	int no, push, boo, comment, parent, author_no;
	char type;
	String content, author, title;
	Date time;
	
	String[] cmt_content, cmt_author;
	Date[] cmt_time;
	int[] cmt_author_no;
	char[] cmt_type;
	
	public Article(String _no, String _type, String _content, String _push, String _boo, String _comment, 
			String _time,String _parent, String _author_no) throws ParseException
	{
		no = Integer.parseInt(_no);
		type = _type.charAt(0);
		content = _content;
		push = Integer.parseInt(_push);
		boo = Integer.parseInt(_boo);
		comment = Integer.parseInt(_comment);
		time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(_time);
		parent = Integer.parseInt(_parent);
		author_no = Integer.parseInt(_author_no);
		
		int total_cmt = push+boo+comment;
		cmt_author = new String[total_cmt];
		cmt_content = new String[total_cmt];
		cmt_author_no = new int[total_cmt];
		cmt_time = new Date[total_cmt];
		cmt_type = new char[total_cmt];
	}
	public void setTitle(String s)
	{
		title = s;
	}
	public void setAuthorName(String s)
	{
		author = s;
	}
	public void setComment(int idx, String _author, String _content, String _time, String _type, String _author_no) throws ParseException
	{
		cmt_author[idx] = _author;
		cmt_content[idx] = _content;
		cmt_time[idx] = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(_time);
		cmt_type[idx] = _type.charAt(0);
		cmt_author_no[idx] = Integer.parseInt(_author_no);
	}
}
