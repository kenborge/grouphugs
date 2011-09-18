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

public class RssNews {

	private Grouphug bot;

	private List<RSSHolder> rssList;

	public RssNews(ModuleHandler handler) {
		bot = Grouphug.getInstance();
		rssList = new ArrayList<RSSHolder>();
		addRss("Star Wars Norge", "http://www.starwarsnorge.com/index.php?action=.xml;type=rss2;sa=news;board=29;limit=20");
		addRss("Darth Hater", "http://darthhater.com/feed");
		addRss("Star Wars: The Old Republic", "http://www.swtor.com/feed/news/all");
		Timer timer = new Timer();
		Task task = new Task("#starwarsnorgea");
		timer.schedule(task, 1*60*1000, 5*60*1000);
	}

	private void addRss(String tag, String urlString) {
		try {
			RSSHolder rss = new RSSHolder();
			rss.url = new URL(urlString);
			rss.tag = tag;
			getLastItemDate(rss);
			rssList.add(rss);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	private SyndFeed getFeed(URL url) {
		SyndFeedInput input = new SyndFeedInput();
		try {
			return input.build(new XmlReader(url));
		} catch (Exception e) {
			e.printStackTrace();
			bot.msg("Kenji", "RSS News Error");
			bot.msg("Kenji", e.toString());
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
		private String channel;

		private Task(String channel) {
			this.channel = channel;
		}

		public void run() {
			System.out.println("RssNews checking for new entries");
			for(RSSHolder rss : rssList) {
				SyndFeedInput input = new SyndFeedInput();
				SyndFeed feed = getFeed(rss.url);
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
						bot.msg(channel, "NEWS | " + rss.tag + " >> " + entry.getTitle() +  " >> " + entry.getLink());
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
		public URL url;
		public String tag;
		public DateTime lastItemDate;
	}
}
