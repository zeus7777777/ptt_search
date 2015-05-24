package ptt_search;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import main.KafkaTopicProducer;

public class PTTSearch
{
	MysqlDB mysql;
	String startTime, endTime, timeQuery;
	ArrayList<String> keywords, wordno, articles, rank, rank_cnt;
	ArrayList<Boolean> is_and;
	ArrayList<Article> all_articles;
	StringBuilder result_str;
	public PTTSearch(String request) throws ParseException, ClassNotFoundException, SQLException
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss"), 
						 pdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		keywords = new ArrayList<String>();
		wordno = new ArrayList<String>();
		articles = new ArrayList<String>();
		is_and = new ArrayList<Boolean>();
		int i;
		String[] str = request.split(","); 
		startTime = pdf.format(sdf.parse(str[0]));
		endTime = pdf.format(sdf.parse(str[1]));
		keywords.add(str[2]);
		for(i=3;i<str.length;i+=2)
		{
			is_and.add(str[i].equals("true")?true:false);
			keywords.add(str[i+1]);
		}
		mysql = new MysqlDB();
		timeQuery = "(TIME>='"+startTime+"' and TIME<='"+endTime+"')";
	}
	public void search() throws SQLException, ParseException
	{
		int i, j;
		String q;
		ResultSet tmp, tmp2;
		//ArrayList<String> new_articles = new ArrayList<String>();
		// find all word_no
		// find articles with relation
		q = "select * from ARTICLE where NO in (select ARTICLE_NO from RELATION where WORD_ID=(select NO from GLOSSARY where WORD='"+keywords.get(0)+"') and "+timeQuery+")";
		tmp = mysql.query(q);
		while(tmp.next())
		{
			if(tmp.getString("TYPE").equals("t"))
			{
				if(!articles.contains(tmp.getString("NO")))
						articles.add(tmp.getString("NO"));
			}
			else
			{
				q = "select * from ARTICLE where NO='"+tmp.getString("PARENT")+"'";
				tmp2 = mysql.query(q);
				while(tmp2.next() && !tmp2.getString("TYPE").equals("t"))
				{
					q = "select * from ARTICLE where NO='"+tmp2.getString("PARENT")+"'";
					tmp2 = mysql.query(q);
				}
				if(!articles.contains(tmp2.getString("NO")))
					articles.add(tmp2.getString("NO"));
			}
		}
		//get root articles
		//get all articles title(no)
		for(i=0;i<articles.size();i++)	System.out.print(articles.get(i)+" "); //--
		
		//get each article
		result_str = new StringBuilder();
		all_articles = new ArrayList<Article>();
		result_str.append("{\"Articles\":[");///
		for(i=0;i<articles.size();i++)
		{
			if(i>0)
				result_str.append(",\n\n");///
			all_articles.add(getArticle(Integer.parseInt(articles.get(i))));
			printArticle(all_articles.get(i));
		}
		
		result_str.append("],\n\"Rank\":[");
		//get rank
		
		rank = new ArrayList<String>();
		rank_cnt = new ArrayList<String>();
		q = "select WORD,count(*) as cnt from RELATION,GLOSSARY where RELATION.WORD_ID=GLOSSARY.NO  and ARTICLE_NO in (select ARTICLE_NO from RELATION where WORD_ID=(select NO from GLOSSARY where WORD='"+keywords.get(0)+"')) group by WORD_ID order by cnt desc limit 100";
		tmp = mysql.query(q);
		while(tmp.next())
		{
			rank.add(tmp.getString("WORD"));
			rank_cnt.add(tmp.getString("cnt"));
		}
		for(i=0;i<rank.size();i++)
		{
			if(i>0)
				result_str.append(",\n");
			result_str.append("{\"Word\":\""+rank.get(i)+"\",\"Count\":\""+rank_cnt.get(i)+"\"}");
		}
		result_str.append("]}");///
		System.out.print("\n\n"+result_str);
		sendMessage(result_str.toString());
	}
	public Article getArticle(int index) throws ParseException, SQLException
	{
		String q, t="";
		Article aa = null;
		ResultSet tmp, tmp2;
		int total, cnt;
		
		q = "select * from ARTICLE where NO='"+index+"'";
		tmp = mysql.query(q);
		if(tmp.next())
		{
			t = tmp.getString("CONTENT");
		}
		
		q = "select * from ARTICLE where type='r' and PARENT='"+index+"'";
		tmp = mysql.query(q);
		if(tmp.next())
		{
			aa = new Article(tmp.getString("NO"), tmp.getString("TYPE"), tmp.getString("CONTENT"),
					tmp.getString("PUSH"), tmp.getString("BOO"), tmp.getString("COMMENT"),
					tmp.getString("TIME"), tmp.getString("PARENT"), tmp.getString("AUTHOR_NO"));
		}
		aa.setTitle(t);
		
		q = "select * from AUTHOR where NO='"+tmp.getString("AUTHOR_NO")+"'";
		tmp = mysql.query(q);
		if(tmp.next())
		{
			aa.setAuthorName(tmp.getString("AUTHOR"));
		}
		
		total = aa.push + aa.boo + aa.comment;
		cnt = 0;
		q = "select * from ARTICLE where PARENT='"+index+"' and type='c'";
		tmp = mysql.query(q);
		while(tmp.next())
		{
			q = "select AUTHOR from AUTHOR where NO='"+tmp.getString("AUTHOR_NO")+"'";
			tmp2 = mysql.query(q);
			aa.setComment(cnt++, tmp2.next()?tmp2.getString("AUTHOR"):"", tmp.getString("CONTENT"), tmp.getString("TIME"),
					tmp.getString("TYPE"), tmp.getString("AUTHOR_NO")); 
		}
		return aa;
	}
	public void printArticle(Article aa)
	{
		System.out.print("====================\n");
		result_str.append("{\n");
		int i;
		System.out.print("標題： "+aa.title+"\n作者： "+aa.author+"\n時間： "+aa.time+"\n\n"+aa.content+"\n\n");
		result_str.append("\"Title\":\""+aa.title+"\",\n\"Author\":\""+aa.author+"\",\n\"Time\":\""+aa.time+"\",\n\"Content\":\""+aa.content+"\",");///
		result_str.append("\n\"Comments\":[\n");///
		for(i=0;i<aa.cmt_author.length;i++)
		{
			if(i>0)
				result_str.append(",");
			System.out.print(aa.cmt_type[i]+"  "+aa.cmt_author[i]+"： "+aa.cmt_content[i]+" "+aa.cmt_time[i]+"\n");
			result_str.append("{\"Author\":\""+aa.cmt_author[i]+"\",\"Content\":\""+aa.cmt_content[i]+"\",\"Time\":\""+aa.cmt_time[i]+"\"}\n");
		}
		result_str.append("]");
		result_str.append("}");
		System.out.print("====================\n");
	}
	public void sendMessage(String str)
	{
		KafkaTopicProducer.getInstance().send("ptt_response", str);
	}
}
