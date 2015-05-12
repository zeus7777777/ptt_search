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
	ArrayList<String> keywords, wordno, articles;
	ArrayList<Boolean> is_and;
	ArrayList<Article> all_articles;
	StringBuilder result_str;
	public PTTSearch(String request) throws ParseException, ClassNotFoundException, SQLException
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss"), pdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
	public void search() throws SQLException, NumberFormatException, ParseException
	{
		int i, j;
		String q;
		ResultSet tmp;
		ArrayList<String> new_articles = new ArrayList<String>();
		// find all word_no
		for(i=0;i<keywords.size();i++)
		{
			q = "select NO from GLOSSARY where WORD='"+keywords.get(i)+"'";
			tmp = mysql.query(q);
			if(tmp.next())
			{
				wordno.add(tmp.getString("NO"));
			}
		}
		
		// find articles with relation
		q = "select ARTICLE_NO from RELATION where WORD_ID='"+wordno.get(0)+"'";
		tmp = mysql.query(q);
		while(tmp.next())
		{
			articles.add(tmp.getString("ARTICLE_NO"));
		}
		for(i=1;i<wordno.size();i++)
		{
			q = "select ARTICLE_NO from RELATION where WORD_ID='"+wordno.get(i)+"'";
			tmp = mysql.query(q);
			new_articles.clear();
			while(tmp.next())
			{
				new_articles.add(tmp.getString("ARTICLE_NO"));
			}
			if(is_and.get(i-1))
			{
				for(j=new_articles.size()-1;j>=0;j--)
					if(!articles.contains(new_articles.get(j)))
						new_articles.remove(j);
			}
			else
			{
				for(j=0;j<articles.size();j++)
					if(!new_articles.contains(articles.get(j)))
						new_articles.add(articles.get(j));
			}
			articles = new_articles;
		}
		for(i=0;i<articles.size();i++)	System.out.print(articles.get(i)+" ");
		
		//get root articles
		new_articles.clear();
		for(i=0;i<articles.size();i++)
		{
			q = "select * from ARTICLE where NO='"+articles.get(i)+"'";
			tmp = mysql.query(q);
			while(tmp.next() && !tmp.getString("PARENT").equals("-1"))
			{
				q = "select * from ARTICLE where NO='"+tmp.getString("PARENT")+"'";
				tmp = mysql.query(q);
			}
			if(!new_articles.contains(tmp.getString("NO")))
				new_articles.add(tmp.getString("NO"));
		}
		articles = new_articles;
		for(i=0;i<articles.size();i++)	System.out.print(articles.get(i)+" ");
		
		//get all articles title(no)
		for(i=0;i<articles.size();i++)
		{
			q = "select NO from ARTICLE where "+timeQuery+" and TYPE='t' and PARENT='"+articles.get(i)+"'";
			tmp = mysql.query(q);
			while(tmp.next())
				if(!articles.contains(tmp.getString("NO")))
					articles.add(tmp.getString("NO"));
		}
		for(i=0;i<articles.size();i++)	System.out.print(articles.get(i)+" "); //--
		
		//get each article
		result_str = new StringBuilder();
		all_articles = new ArrayList<Article>();
		result_str.append("[");///
		for(i=0;i<articles.size();i++)
		{
			if(i>0)
				result_str.append(",\n\n");///
			all_articles.add(getArticle(Integer.parseInt(articles.get(i))));
			printArticle(all_articles.get(i));
		}
		result_str.append("]");///
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
		result_str.append("標題:"+aa.title+",\n作者:"+aa.author+",\n時間:"+aa.time+",\n內文:"+aa.content+",");///
		result_str.append("\n回應:[\n");///
		for(i=0;i<aa.cmt_author.length;i++)
		{
			if(i>0)
				result_str.append(",");
			System.out.print(aa.cmt_type[i]+"  "+aa.cmt_author[i]+"： "+aa.cmt_content[i]+" "+aa.cmt_time[i]+"\n");
			result_str.append("{作者:"+aa.cmt_author[i]+",內文:"+aa.cmt_content[i]+",時間:"+aa.cmt_time[i]+"}\n");
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
