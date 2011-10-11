package no.kvikshaug.gh.modules;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

import com.sun.jndi.url.corbaname.corbanameURLContextFactory;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.exceptions.SQLUnavailableException;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQLHandler;

public class RssNews implements TriggerListener {
	private static final String TRIGGER_LIST = "rsslist";
	private static final String TRIGGER_REMOVE = "rssremove";
	private static final String TRIGGER_ADD = "rssadd";
	//(WEB url pattern \b((?:https?://|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,4}/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()\[\]{};:'".,<>?«»“”‘’]))
	private Pattern patternAddMessage = Pattern.compile("^(.*)\\s+((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:\'\".,<>?«»“”‘’]))\\s*$", Pattern.DOTALL);


	//CREATE TABLE rss(id INTEGER PRIMARY KEY, tag TEXT, url TEXT, channel TEXT);
	private Grouphug bot;

	private List<RSSHolder> rssList;

	private SQLHandler sqlHandler;

	public RssNews(ModuleHandler handler) {
		bot = Grouphug.getInstance();
		rssList = new ArrayList<RSSHolder>();

		try {
			sqlHandler = SQLHandler.getSQLHandler();
			List<Object[]> rows = sqlHandler.select("SELECT `id`, `tag`, `url`, `channel` FROM rss;");
			for(Object[] row : rows) {
				int id = (Integer)row[0];
				String tag = (String) row[1];
				String url = (String) row[2];
				String channel = (String) row[3];

				initRss(id, tag, url, channel);
				System.out.println("Loaded rss from database: " + tag);
			}
		} catch(SQLUnavailableException ex) {
			System.err.println("RssNews startup: SQL is unavailable!");
			return;
		} catch (SQLException e) {
			e.printStackTrace();
		}

		handler.addTriggerListener(TRIGGER_ADD, this);
		handler.addTriggerListener(TRIGGER_REMOVE, this);
		handler.addTriggerListener(TRIGGER_LIST, this);
		
		Timer timer = new Timer();
		Task task = new Task();
		timer.schedule(task, 1*60*1000, 5*60*1000);
	}

	@Override
	public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
		System.out.println(channel + " " +trigger + " " + message);
		if(trigger.equals(TRIGGER_ADD)) {
			Matcher matcher = patternAddMessage.matcher(message);
			if(matcher != null && matcher.matches()) {
				String tag = matcher.group(1);
				String url = matcher.group(2);

				int id = saveRss(tag, url, channel);
				if(id == 0) {
					bot.msg(channel, "Saving rss failed (SqlError)");
				}else {
					if(initRss(id, tag, url, channel)) {
						bot.msg(channel, "Now tracking feed with tag: " + tag);
					} else {
						deleteRss(id);
						bot.msg(channel, "Saving rss failed (Invalid URL)");
					}
				}
				return;
			}

		} else if(trigger.equals(TRIGGER_REMOVE)) {
			message = message.trim();
			try {
				int id = Integer.parseInt(message);
				if(deleteRss(id) > 0) {
					for(RSSHolder rss : rssList) {
						if(rss.id == id) { 
							rssList.remove(rss);
							break;
						}
					}
					bot.msg(channel, "Deleted rss with id " + id);
				} else {
					bot.msg(channel, "Something is not right, did not delete anything");
				}
				return;
			}catch (NumberFormatException e) {
				e.printStackTrace();
				bot.msg(channel, "Not a valid id");
				return;
			}

		} else if(trigger.equals(TRIGGER_LIST)) {
			if(rssList.size() == 0) bot.msg(channel, "Not following any Rss feeds");
			for(int i=0;i<rssList.size();i++) {
				if(i > 5) break;
				RSSHolder rss = rssList.get(i);
				bot.msg(channel, "Id " + rss.id + " Tag: " + rss.tag + " Url: " + rss.url + " Channel: " + rss.channel);
			}
			return;
		}
		bot.msg(channel, "Error: Something was invalide with the command");
	}

	private boolean initRss(int id, String tag, String urlString, String channel) {
		try {
			RSSHolder rss = new RSSHolder();
			rss.id = id;
			rss.url = new URL(urlString);
			rss.tag = tag;
			rss.channel = channel;
			getLastItemDate(rss);
			rssList.add(rss);
			return true;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		}
	}

	private int saveRss(String tag, String url, String channel) {
		int id = -1;
		try {
			List<String> params = new ArrayList<String>();
			params.add(tag);
			params.add(url);
			params.add(channel);
			id = sqlHandler.insert("INSERT INTO rss (`tag`, `url`, `channel`) VALUES (?, ?, ?);", params);
		} catch(SQLException e) {
			System.err.println("RSSNews insertion: SQL Exception: "+e);
		}
		return id;
	}

	private int deleteRss(int id) {
		try {
			return sqlHandler.delete("DELETE FROM rss WHERE `id` = '"+id+"';");
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
	}

	private SyndFeed getFeed(URL url) {
		SyndFeedInput input = new SyndFeedInput();
		try {
			return input.build(new XmlReader(url));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void getLastItemDate(RSSHolder rss) {
		SyndFeed feed = getFeed(rss.url);
		SyndEntry entry = (SyndEntry)feed.getEntries().get(0);
		if(entry.getPublishedDate() != null ) rss.lastItemDate = new DateTime(entry.getPublishedDate());
		else if(entry.getUpdatedDate() != null) rss.lastItemDate = new DateTime(entry.getUpdatedDate());
		else throw new RuntimeException("Rss feed entries has no date");
	}

	private class Task extends TimerTask {

		public void run() {
			System.out.println("RssNews checking for new entries");
			for(RSSHolder rss : rssList) {
				SyndFeedInput input = new SyndFeedInput();
				SyndFeed feed = getFeed(rss.url);
				if(feed == null) {
					if(rss.errorCount == 5)
						bot.msg(rss.channel, "RSS News " + rss.tag + " has failed 5 times in a row!");
					else if(rss.errorCount == 72) //6 hours
						bot.msg(rss.channel, "RSS News " + rss.tag + " has failed the last 6h!");
					else if(rss.errorCount % 288 == 0) //a day
						bot.msg(rss.channel, "RSS News " + rss.tag + " has failed all day!");
					rss.errorCount += 1;
					continue;
				}
				if(rss.errorCount > 0) rss.errorCount = 0;
				boolean foundNew = false;
				for (Iterator iter = feed.getEntries().iterator(); iter.hasNext();) {
					SyndEntry entry = (SyndEntry)iter.next();
					DateTime entryDate;
					if(entry.getPublishedDate() != null ) entryDate = new DateTime(entry.getPublishedDate());
					else if(entry.getUpdatedDate() != null) entryDate = new DateTime(entry.getUpdatedDate());
					else {
						System.out.println("Rss entry has no date " + entry.getTitle() + " " + rss.tag);
						continue;
					}
					if(entryDate.isAfter(rss.lastItemDate)) {
						foundNew = true;
						bot.msg(rss.channel, "NEWS | " + rss.tag + " >> " + entry.getTitle() +  " >> " + entry.getLink());
					}
				}
				if(foundNew) {
					if(((SyndEntry)feed.getEntries().get(0)).getPublishedDate() != null ) rss.lastItemDate = new DateTime(((SyndEntry)feed.getEntries().get(0)).getPublishedDate());
					else if(((SyndEntry)feed.getEntries().get(0)).getUpdatedDate() != null) rss.lastItemDate = new DateTime(((SyndEntry)feed.getEntries().get(0)).getUpdatedDate());
					else rss.lastItemDate = new DateTime();
				}
			}
		}
	}

	private class RSSHolder {
		public int id;
		public URL url;
		public String tag;
		public DateTime lastItemDate;
		public String channel;
		public int errorCount = 0;
	}
}
