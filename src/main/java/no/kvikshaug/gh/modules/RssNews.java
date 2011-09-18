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
    private DateTime lastTime;
    private URL feedUrl;

    public RssNews(ModuleHandler handler) {
    	bot = Grouphug.getInstance();
    	lastTime = new DateTime();
    	
    	SyndFeedInput input = new SyndFeedInput();
		SyndFeed feed;
		try {
			feed = input.build(new XmlReader(feedUrl));
			lastTime = new DateTime(((SyndEntry)feed.getEntries().get(1)).getPublishedDate());
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
    	try {
			feedUrl = new URL("http://www.starwarsnorge.com/index.php?action=.xml;type=rss2;sa=news;board=29;limit=20");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
        Timer timer = new Timer();
        Task task = new Task("#starwarsnorge");
        timer.schedule(task, new Period(0, 1, 0, 0).getMillis(), new Period(0, 5, 0, 0).getMillis());
    }

    private class Task extends TimerTask {
        private String channel;

        private Task(String channel) {
            this.channel = channel;
            new Thread(this).start();
        }

        public void run() {
            try {
    			SyndFeedInput input = new SyndFeedInput();
    			SyndFeed feed = input.build(new XmlReader(feedUrl));
    			boolean foundNew = false;
    			for (Iterator iter = feed.getEntries().iterator(); iter.hasNext();) {
    				SyndEntry entry = (SyndEntry)iter.next();
    				if(new DateTime(entry.getPublishedDate()).isAfter(lastTime)) {
    					System.out.println("piss");
    					bot.msg(channel, "Star Wars Norge Nyheter" + ": " + entry.getTitle() +  " >> " + entry.getLink());
    				}
    			}
    			if(foundNew)
    				lastTime = new DateTime(((SyndEntry)feed.getEntries().get(0)).getPublishedDate());
    		}
    		catch (Exception ex) {
    			ex.printStackTrace();
    			bot.msg(channel, "RSS News Error");
    		}           
        }
    }
}
